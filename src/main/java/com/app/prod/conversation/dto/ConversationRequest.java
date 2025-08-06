package com.app.prod.conversation.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversationRequest(
        Boolean isGroup,
        String name
) {
}
