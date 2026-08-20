package com.app.prod.interaction.dto;

import com.app.prod.interaction.enums.InteractionType;
import com.app.prod.user.dto.UserData;

import java.time.LocalDateTime;

public record InteractionUserResponse(
        UserData user,
        InteractionType interactionType,
        LocalDateTime createdAt
) {
}
