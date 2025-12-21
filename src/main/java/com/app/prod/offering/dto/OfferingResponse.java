package com.app.prod.offering.dto;

import com.app.prod.user.dto.BasicUserData;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OfferingResponse(
        UUID id,
        String title,
        String description,
        String category,
        boolean isActive,
        BigDecimal price,
        LocalDateTime endDate,
        LocalDateTime createdAt,
        boolean createdByUser,
        BasicUserData providerData
) {
}
