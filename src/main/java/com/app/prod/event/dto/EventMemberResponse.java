package com.app.prod.event.dto;

import com.app.prod.event.enums.EventMemberStatus;

import java.util.List;
import java.util.UUID;

public record EventMemberResponse(
        String displayName,
        boolean isHost,
        EventMemberStatus status,
        List<UUID> preferredSlots,
        List<UUID> optionalSlots
) {
}
