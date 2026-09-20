package com.app.prod.event.dto;

import com.app.prod.event.enums.EventMemberRole;
import com.app.prod.event.enums.EventMemberStatus;

import java.util.UUID;

public record EventMemberRow(
        UUID id,
        String displayName,
        EventMemberRole role,
        EventMemberStatus status
) {
}
