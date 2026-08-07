package com.app.prod.interaction.dto;

import com.app.prod.interaction.enums.InteractionEntityType;
import com.app.prod.interaction.enums.InteractionType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InteractionRequest(
        @NotNull InteractionEntityType entityType,
        @NotNull UUID entityId,
        @NotNull InteractionType interactionType
) {
}
