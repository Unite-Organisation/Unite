package com.app.prod.event.service;

public record CreatedEvent(
        String slug,
        String returnCode,
        String sessionToken
) {
}
