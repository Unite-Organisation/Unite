package com.app.prod.event.web;

import com.app.prod.event.enums.EventIdentityOrigin;
import com.app.prod.event.enums.EventMemberRole;

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
