package com.app.prod.event.dto;

import com.app.prod.event.enums.EventMemberStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record EventMemberResponse(
        UUID id,
        String displayName,
        boolean isHost,
        EventMemberStatus status,
        List<UUID> preferredSlots,
        List<UUID> optionalSlots,
        LocalDateTime joinedAt
) {
}
