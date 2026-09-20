package com.app.prod.facilities.dto;

import com.app.prod.facilities.enums.ReservationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        UUID facilityId,
        UUID userId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        ReservationStatus status,
        String purpose,
        LocalDateTime createdAt
) {
}
