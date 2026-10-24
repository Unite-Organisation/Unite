package com.app.prod.event.events;

import com.app.prod.eventbus.AppEvent;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Only one date is left to gather people on. Either the voting deadline passed with nothing at the
 * threshold, or a group fell apart once there was no time left to choose another date - then
 * {@code brokenUpGroupConfirmBy} is the walk-out window of that group, and {@code null} otherwise.
 * The second can happen more than once to the same event, the first only once.
 */
public record EventReducedToOneSlotEvent(
        UUID eventId,
        UUID slotId,
        LocalDateTime brokenUpGroupConfirmBy
) implements AppEvent {
}
