package com.app.prod.event.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateEventRequest(
        @NotBlank @Size(max = 256) String name,
        String description,
        LocalDateTime startDate,
        LocalDateTime endDate,
        @Size(max = 256) String location,
        @Size(max = 2048) String onlineUrl,
        @Positive Integer maxAttendees,
        boolean waitlistEnabled,
        @Size(max = 60) String displayName,
        @Valid DeviceFingerprint device
) {
}
