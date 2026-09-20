package com.app.prod.event.dto;

import com.app.prod.event.enums.EventIdentityOrigin;
import com.app.prod.event.enums.EventMemberRole;
import com.app.prod.event.enums.EventMemberStatus;

public record MemberResponse(
        String displayName,
        EventMemberRole role,
        EventMemberStatus status,
        EventIdentityOrigin origin
) {
}
