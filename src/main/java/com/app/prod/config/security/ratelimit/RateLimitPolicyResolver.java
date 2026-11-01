package com.app.prod.config.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class RateLimitPolicyResolver {

    public static final String AUTH = "auth";
    public static final String EVENT_SESSION = "event-session";
    public static final String EVENT_CREATE = "event-create";
    public static final String SHARE = "share";
    public static final String PUBLIC_READ = "public-read";
    public static final String DEFAULT = "default";

    private static final Pattern EVENT_SESSION_PATH = Pattern.compile("^/public/event/[^/]+/session$");

    public String resolve(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return null;
        }

        String path = request.getServletPath();
        if (path == null) {
            return DEFAULT;
        }

        if (path.startsWith("/internal/")
                || path.equals("/healthcheck")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")) {
            return null;
        }

        boolean post = HttpMethod.POST.matches(request.getMethod());
        if (post && EVENT_SESSION_PATH.matcher(path).matches()) {
            return EVENT_SESSION;
        }
        if (post && path.equals("/public/event")) {
            return EVENT_CREATE;
        }
        if (path.startsWith("/public/share")) {
            return SHARE;
        }
        if (path.startsWith("/public/")) {
            return PUBLIC_READ;
        }
        if (path.startsWith("/auth/")) {
            return AUTH;
        }
        return DEFAULT;
    }
}
