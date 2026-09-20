package com.app.prod.event.dto;

import com.app.prod.event.enums.EventMemberRole;

import java.util.UUID;

public record SessionMember(
        UUID eventId,
        UUID memberId,
        EventMemberRole role,
        UUID userId
) {
}
