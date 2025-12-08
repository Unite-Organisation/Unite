package com.app.prod.conversation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record AddMembersRequest(
        @NotNull
        UUID conversationId,
        @NotNull
        @NotEmpty
        List<UUID> ids
) {
}
