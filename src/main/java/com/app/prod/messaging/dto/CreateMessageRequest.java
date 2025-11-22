package com.app.prod.messaging.dto;

import java.util.UUID;

public record CreateMessageRequest(
        UUID conversationId,
        String content
) {
}
