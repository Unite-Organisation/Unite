package com.app.prod.requests.dto;

import com.app.prod.requests.enums.RequestStatus;

import com.app.prod.user.dto.UserData;

import java.time.LocalDateTime;
import java.util.UUID;

public record RequestResponse(
        UUID requestId,
        String title,
        String description,
        UserData userIdNeed,
        UserData userDonor,
        RequestStatus status,
        LocalDateTime createdAt,
        LocalDateTime deadlineAt
) {
}
