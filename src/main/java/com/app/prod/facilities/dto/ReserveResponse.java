package com.app.prod.facilities.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReserveResponse(
        boolean success,
        List<ReservationResponse> reservations
){
    public record ReservationResponse(
            UUID id,
            UUID facilityId,
            UUID userId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String status,
            String purpose,
            LocalDateTime createdAt
    ) {
    }
}
