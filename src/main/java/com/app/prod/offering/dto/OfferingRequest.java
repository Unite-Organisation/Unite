package com.app.prod.offering.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OfferingRequest(
        @NotEmpty
        String title,

        @NotEmpty
        String description,
        String category,
        boolean isActive,

        @Min(0)
        BigDecimal price,
        LocalDateTime endDate
) {
}
