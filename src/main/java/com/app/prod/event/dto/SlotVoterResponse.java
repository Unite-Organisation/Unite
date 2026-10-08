package com.app.prod.event.dto;

import com.app.prod.event.enums.SlotPreference;

public record SlotVoterResponse(
        String displayName,
        SlotPreference preference
) {
}
