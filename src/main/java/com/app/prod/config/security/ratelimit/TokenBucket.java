package com.app.prod.config.security.ratelimit;

/**
 * A single token bucket: {@code capacity} tokens that refill continuously at
 * {@code refill} tokens per {@code periodNanos}. One request costs one token.
 * <p>
 * Thread-safe through a short synchronized section - buckets are per key
 * (policy + client ip), so contention is limited to requests from the same
 * source hitting the same group of endpoints.
 */
final class TokenBucket {

    private final double capacity;
    private final double tokensPerNano;

    private double tokens;
    private long lastRefillNanos;
    private volatile long lastAccessNanos;

    TokenBucket(int capacity, int refillTokens, long periodNanos, long nowNanos) {
        this.capacity = capacity;
        this.tokensPerNano = (double) refillTokens / periodNanos;
        this.tokens = capacity;          // start full: a first-time caller gets the full burst
        this.lastRefillNanos = nowNanos;
        this.lastAccessNanos = nowNanos;
    }

    synchronized Decision tryConsume(long nowNanos) {
        refill(nowNanos);
        this.lastAccessNanos = nowNanos;
        if (tokens >= 1.0) {
            tokens -= 1.0;
            return new Decision(true, 0);
        }
        double missing = 1.0 - tokens;
        long waitNanos = (long) Math.ceil(missing / tokensPerNano);
        long retryAfterSeconds = Math.max(1, (long) Math.ceil(waitNanos / 1_000_000_000.0));
        return new Decision(false, retryAfterSeconds);
    }

    long lastAccessNanos() {
        return lastAccessNanos;
    }

    private void refill(long nowNanos) {
        long elapsed = nowNanos - lastRefillNanos;
        if (elapsed <= 0) {
            return;
        }
        tokens = Math.min(capacity, tokens + elapsed * tokensPerNano);
        lastRefillNanos = nowNanos;
    }

    record Decision(boolean allowed, long retryAfterSeconds) {
    }
}
