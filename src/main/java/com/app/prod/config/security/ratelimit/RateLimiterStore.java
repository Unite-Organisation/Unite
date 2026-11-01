package com.app.prod.config.security.ratelimit;

import com.app.prod.config.security.ratelimit.RateLimitProperties.Policy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;


@Component
@Slf4j
public class RateLimiterStore {

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final AtomicBoolean evicting = new AtomicBoolean(false);

    public TokenBucket.Decision tryConsume(String key, Policy policy, int maxBuckets, long idleTtlNanos) {
        long now = System.nanoTime();
        if (buckets.size() >= maxBuckets) {
            evictIfNeeded(maxBuckets, idleTtlNanos, now);
        }
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(policy.capacity(), policy.refill(), policy.period().toNanos(), now));
        return bucket.tryConsume(now);
    }

    int size() {
        return buckets.size();
    }

    private void evictIfNeeded(int maxBuckets, long idleTtlNanos, long now) {
        // one thread sweeps at a time; the rest just carry on and may add past the cap briefly
        if (!evicting.compareAndSet(false, true)) {
            return;
        }
        try {
            buckets.entrySet().removeIf(e -> now - e.getValue().lastAccessNanos() > idleTtlNanos);
            int overflow = buckets.size() - maxBuckets;
            if (overflow > 0) {
                buckets.entrySet().stream()
                        .sorted(Comparator.comparingLong(e -> e.getValue().lastAccessNanos()))
                        .limit(overflow)
                        .map(Map.Entry::getKey)
                        .toList()
                        .forEach(buckets::remove);
                log.warn("Rate limiter bucket map hit the cap ({}); evicted {} least-recently-used key(s)", maxBuckets, overflow);
            }
        } finally {
            evicting.set(false);
        }
    }
}
