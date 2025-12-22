package com.app.prod.conversation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record GroupConversationRequest(
        String name,
        @NotNull @Size(min = 2, message = "Group conversation must have at least 2 members")
        List<UUID> members
) {
}
