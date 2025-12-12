package com.app.prod.conversation.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversationMessageResponse(
        UUID author,
        String authorName,
        String authorLastName,
        LocalDateTime sendAt,
        String content
) {
}
