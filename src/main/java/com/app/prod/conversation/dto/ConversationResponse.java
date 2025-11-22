package com.app.prod.conversation.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        Boolean isGroup,
        String name,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
