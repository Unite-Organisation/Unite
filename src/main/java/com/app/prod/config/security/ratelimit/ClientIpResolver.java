package com.app.prod.config.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClientIpResolver {

    private final RateLimitProperties properties;

    public String resolve(HttpServletRequest request) {
        int trusted = properties.getTrustedProxies();
        if (trusted <= 0) {
            return request.getRemoteAddr();
        }

        String header = request.getHeader("X-Forwarded-For");
        if (header == null || header.isBlank()) {
            return request.getRemoteAddr();
        }

        String[] parts = header.split(",");
        // the ip the outermost trusted proxy observed sits `trusted` hops from the right
        int index = parts.length - trusted;
        if (index < 0) {
            index = 0;
        }
        String ip = parts[index].trim();
        return ip.isEmpty() ? request.getRemoteAddr() : ip;
    }
}
