package com.app.prod.event.events;

import com.app.prod.eventbus.AppEvent;

import java.util.UUID;

public record EventReducedToOneSlotEvent(
        UUID eventId,
        UUID slotId
) implements AppEvent {
}
