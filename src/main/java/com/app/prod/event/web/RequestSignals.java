package com.app.prod.event.web;

public record RequestSignals(
        String ipAddress,
        String userAgent,
        String acceptLanguage
) {
}
