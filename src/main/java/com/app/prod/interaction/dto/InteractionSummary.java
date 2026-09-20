package com.app.prod.interaction.dto;

import com.app.prod.interaction.enums.InteractionType;

public record InteractionSummary(
        InteractionType interactionType,
        int count,
        boolean reactedByCurrentUser
) {
}
