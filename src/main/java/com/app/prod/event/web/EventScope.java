package com.app.prod.event.web;

import com.app.prod.event.enums.EventIdentityOrigin;
import com.app.prod.event.enums.EventMemberRole;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.UnauthorizedDataAccessException;

import java.util.UUID;

public final class EventScope {

    private final UUID eventId;
    private final UUID memberId;
    private final EventMemberRole role;
    private final EventIdentityOrigin origin;

    EventScope(UUID eventId, UUID memberId, EventMemberRole role, EventIdentityOrigin origin) {
        this.eventId = eventId;
        this.memberId = memberId;
        this.role = role;
        this.origin = origin;
    }

    public void requireHost() {
        if (this.role() != EventMemberRole.HOST) {
            throw new UnauthorizedDataAccessException(AppError.of(Code.EVENT_HOST_REQUIRED));
        }
    }

    public void requireMember() {
        if (this.role() != EventMemberRole.MEMBER) {
            throw new UnauthorizedDataAccessException(AppError.of(Code.EVENT_MEMBER_REQUIRED));
        }
    }

    public UUID eventId() {
        return eventId;
    }

    public UUID memberId() {
        return memberId;
    }

    public EventMemberRole role() {
        return role;
    }

    public EventIdentityOrigin origin() {
        return origin;
    }
}
