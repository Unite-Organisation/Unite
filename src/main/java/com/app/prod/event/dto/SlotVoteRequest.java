package com.app.prod.event.dto;

import com.app.prod.event.enums.SlotPreference;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SlotVoteRequest(
        @NotNull UUID slotId,
        @NotNull SlotPreference preference
) {
}
