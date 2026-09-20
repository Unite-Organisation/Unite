package com.app.prod.facilities.dto;

import java.time.LocalDateTime;

public record FacilitySlot(
        LocalDateTime startTime,
        LocalDateTime endTime,
        boolean taken,
        FacilityReservation reservation
) {
    public static FacilitySlot free(LocalDateTime startTime, LocalDateTime endTime) {
        return new FacilitySlot(startTime, endTime, false, null);
    }

    public static FacilitySlot taken(LocalDateTime startTime, LocalDateTime endTime, FacilityReservation reservation) {
        return new FacilitySlot(startTime, endTime, true, reservation);
    }
}
