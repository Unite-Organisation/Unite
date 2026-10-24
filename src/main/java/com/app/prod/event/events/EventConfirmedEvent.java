package com.app.prod.event.events;

import com.app.prod.eventbus.AppEvent;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventConfirmedEvent(
        UUID eventId,
        UUID slotId,
        LocalDateTime startDateTime
) implements AppEvent {
}
