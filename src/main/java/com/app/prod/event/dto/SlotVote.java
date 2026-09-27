package com.app.prod.event.dto;

import com.app.prod.event.enums.SlotPreference;

import java.util.UUID;

public record SlotVote(
        UUID slotId,
        SlotPreference preference
) {
}
