package com.app.prod.facilities.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReservationRequest(
        UUID facilityId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String purpose
) {
}
