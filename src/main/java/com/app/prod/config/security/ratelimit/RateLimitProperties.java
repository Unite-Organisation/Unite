package com.app.prod.config.security.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private int trustedProxies = 0;

    private int maxBuckets = 50_000;
    private Duration idleTtl = Duration.ofMinutes(10);
    private Map<String, Policy> policies = new HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getTrustedProxies() {
        return trustedProxies;
    }

    public void setTrustedProxies(int trustedProxies) {
        this.trustedProxies = trustedProxies;
    }

    public int getMaxBuckets() {
        return maxBuckets;
    }

    public void setMaxBuckets(int maxBuckets) {
        this.maxBuckets = maxBuckets;
    }

    public Duration getIdleTtl() {
        return idleTtl;
    }

    public void setIdleTtl(Duration idleTtl) {
        this.idleTtl = idleTtl;
    }

    public Map<String, Policy> getPolicies() {
        return policies;
    }

    public void setPolicies(Map<String, Policy> policies) {
        this.policies = policies;
    }

    public Policy policy(String name) {
        return policies.get(name);
    }

    public record Policy(int capacity, int refill, Duration period) {
    }
}
