package com.app.prod.config.security.ratelimit;

import com.app.prod.config.security.ratelimit.RateLimitProperties.Policy;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.TooManyRequestsException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

/**
 * Per-ip, per-policy rate limiting. Sits in the security chain right after the CORS
 * filter, so a blocked request is rejected before authentication and any database
 * work, yet the 429 still carries CORS headers for the browser frontend.
 * <p>
 * On a block the response is rendered through {@code HandlerExceptionResolver} - the
 * same route {@code JwtAuthFilter} uses - so the body is a normal {@code ApiErrorResponse}
 * and the status comes from {@code GlobalExceptionHandler}.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;
    private final RateLimitPolicyResolver policyResolver;
    private final RateLimiterStore store;
    private final ClientIpResolver clientIpResolver;
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!properties.isEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        String policyName = policyResolver.resolve(request);
        if (policyName == null) {
            chain.doFilter(request, response);
            return;
        }

        Policy policy = properties.policy(policyName);
        if (policy == null || policy.period() == null || policy.period().isZero()) {
            log.warn("No valid rate-limit policy '{}' configured; letting request through", policyName);
            chain.doFilter(request, response);
            return;
        }

        String ip = clientIpResolver.resolve(request);
        String key = policyName + ":" + ip;
        TokenBucket.Decision decision = store.tryConsume(key, policy, properties.getMaxBuckets(), properties.getIdleTtl().toNanos());

        if (decision.allowed()) {
            chain.doFilter(request, response);
            return;
        }

        log.info("Rate limit '{}' hit by {} on {} {}", policyName, ip, request.getMethod(), request.getServletPath());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()));
        handlerExceptionResolver.resolveException(request, response, null,
                new TooManyRequestsException(decision.retryAfterSeconds(), AppError.of(Code.RATE_LIMIT_EXCEEDED))
        );
    }
}
