package com.app.prod.user.dto;

import com.app.prod.mail.enums.EmailDeliveryStatus;
import com.app.prod.user.enums.UserStatus;

import java.util.UUID;


public record ExistingAccount(
        UUID userId,
        String email,
        UserStatus status,
        EmailDeliveryStatus lastInvitation
) {
    public boolean awaitsAnInvitationThatNeverArrived() {
        return status != UserStatus.ACTIVE && lastInvitation != EmailDeliveryStatus.SENT;
    }
}
