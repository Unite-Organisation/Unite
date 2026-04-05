package com.app.prod.internal.dtos;

import java.util.List;
import java.util.UUID;

public record ConversationBulkActionDto(
        UUID userId,
        List<UUID> contacts
) {
}
