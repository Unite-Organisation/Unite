package com.app.prod.user.dto;

import com.app.prod.mail.dto.DeliverySummary;
import com.app.prod.user.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record BuildingUserResponse(
        UUID id,
        String firstName,
        String lastName,
        String username,
        String email,
        UserStatus status,
        LocalDateTime createdAt,
        DeliverySummary invitation
) {
}
