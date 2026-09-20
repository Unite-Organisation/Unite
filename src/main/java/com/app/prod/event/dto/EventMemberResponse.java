package com.app.prod.event.dto;

import com.app.prod.event.enums.EventMemberStatus;

public record EventMemberResponse(
        String displayName,
        boolean isHost,
        EventMemberStatus status
) {
}
