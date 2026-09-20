package com.app.prod.mail.dto;

import com.app.prod.mail.enums.EmailDeliveryStatus;

import java.time.LocalDateTime;

public record DeliverySummary(
        EmailDeliveryStatus status,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime sentAt
) {
}
