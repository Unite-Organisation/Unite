package com.app.prod.event.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record SlotRequest(
        @NotNull LocalDateTime startDate,
        LocalDateTime endDate
) {
}
