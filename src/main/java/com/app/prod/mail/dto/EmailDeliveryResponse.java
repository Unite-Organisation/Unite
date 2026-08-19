package com.app.prod.mail.dto;

import com.app.prod.mail.enums.EmailDeliveryStatus;
import com.app.prod.mail.enums.EmailDeliveryType;

import java.time.LocalDateTime;
import java.util.UUID;

public record EmailDeliveryResponse(
        UUID id,
        EmailDeliveryType deliveryType,
        UUID referenceId,
        String email,
        EmailDeliveryStatus status,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime sentAt
) {
}
