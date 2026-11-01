package com.app.prod.config.security.ratelimit;

import com.app.prod.config.security.ratelimit.RateLimitProperties.Policy;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitTest {

    private static final long SECOND = 1_000_000_000L;

    // --- TokenBucket ------------------------------------------------------

    @Test
    void allowsUpToCapacityThenBlocks() {
        TokenBucket bucket = new TokenBucket(2, 2, SECOND, 0);

        assertThat(bucket.tryConsume(0).allowed()).isTrue();
        assertThat(bucket.tryConsume(0).allowed()).isTrue();

        TokenBucket.Decision blocked = bucket.tryConsume(0);
        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.retryAfterSeconds()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void refillsOverTime() {
        TokenBucket bucket = new TokenBucket(2, 2, SECOND, 0);
        bucket.tryConsume(0);
        bucket.tryConsume(0);
        assertThat(bucket.tryConsume(0).allowed()).isFalse();

        // a full period later both tokens are back
        assertThat(bucket.tryConsume(SECOND).allowed()).isTrue();
        assertThat(bucket.tryConsume(SECOND).allowed()).isTrue();
        assertThat(bucket.tryConsume(SECOND).allowed()).isFalse();
    }

    @Test
    void storeIsolatesKeys() {
        RateLimiterStore store = new RateLimiterStore();
        Policy policy = new Policy(1, 1, Duration.ofMinutes(1));

        assertThat(store.tryConsume("event-session:1.1.1.1", policy, 1000, SECOND * 600).allowed()).isTrue();
        assertThat(store.tryConsume("event-session:1.1.1.1", policy, 1000, SECOND * 600).allowed()).isFalse();
        // a different ip has its own bucket
        assertThat(store.tryConsume("event-session:2.2.2.2", policy, 1000, SECOND * 600).allowed()).isTrue();
    }

    // --- RateLimitPolicyResolver -----------------------------------------

    @Test
    void resolvesPolicyPerEndpoint() {
        RateLimitPolicyResolver resolver = new RateLimitPolicyResolver();

        assertThat(resolver.resolve(req("POST", "/public/event/abc/session"))).isEqualTo(RateLimitPolicyResolver.EVENT_SESSION);
        assertThat(resolver.resolve(req("POST", "/public/event"))).isEqualTo(RateLimitPolicyResolver.EVENT_CREATE);
        assertThat(resolver.resolve(req("GET", "/public/event/abc/members"))).isEqualTo(RateLimitPolicyResolver.PUBLIC_READ);
        assertThat(resolver.resolve(req("GET", "/public/share/event/abc"))).isEqualTo(RateLimitPolicyResolver.SHARE);
        assertThat(resolver.resolve(req("POST", "/auth/login"))).isEqualTo(RateLimitPolicyResolver.AUTH);
        assertThat(resolver.resolve(req("GET", "/building"))).isEqualTo(RateLimitPolicyResolver.DEFAULT);
    }

    @Test
    void skipsInternalAndPreflight() {
        RateLimitPolicyResolver resolver = new RateLimitPolicyResolver();

        assertThat(resolver.resolve(req("POST", "/internal/jobs/drain"))).isNull();
        assertThat(resolver.resolve(req("GET", "/healthcheck"))).isNull();
        assertThat(resolver.resolve(req("OPTIONS", "/public/event/abc/session"))).isNull();
    }

    // --- ClientIpResolver -------------------------------------------------

    @Test
    void usesSocketAddressWithoutTrustedProxies() {
        ClientIpResolver resolver = new ClientIpResolver(propertiesWithProxies(0));
        MockHttpServletRequest request = req("GET", "/public/event/x");
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Forwarded-For", "1.2.3.4");   // must be ignored

        assertThat(resolver.resolve(request)).isEqualTo("10.0.0.5");
    }

    @Test
    void takesProxyObservedIpAndIgnoresSpoofedPrefix() {
        ClientIpResolver resolver = new ClientIpResolver(propertiesWithProxies(1));
        MockHttpServletRequest request = req("GET", "/public/event/x");
        // client tries to spoof 9.9.9.9; the load balancer appends the real 2.2.2.2
        request.addHeader("X-Forwarded-For", "9.9.9.9, 2.2.2.2");

        assertThat(resolver.resolve(request)).isEqualTo("2.2.2.2");
    }

    private static MockHttpServletRequest req(String method, String servletPath) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, servletPath);
        request.setServletPath(servletPath);
        return request;
    }

    private static RateLimitProperties propertiesWithProxies(int proxies) {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setTrustedProxies(proxies);
        return properties;
    }
}
