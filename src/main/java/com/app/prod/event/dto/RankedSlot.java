package com.app.prod.event.dto;

import java.util.UUID;

public record RankedSlot(
        SlotTally tally,
        boolean qualifies,
        int chance
) {
    public UUID slotId() {
        return tally.slotId();
    }
}
