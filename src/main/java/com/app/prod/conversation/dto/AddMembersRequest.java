package com.app.prod.conversation.dto;

import java.util.List;
import java.util.UUID;

public record AddMembersRequest(
        UUID conversationId,
        List<UUID> ids
) {
}
