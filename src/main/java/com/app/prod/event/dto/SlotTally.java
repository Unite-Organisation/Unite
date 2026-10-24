package com.app.prod.event.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SlotTally(
        UUID slotId,
        LocalDateTime startDateTime,
        int preferred,
        int ifNeeded
) {
    public int total() {
        return preferred + ifNeeded;
    }
}
