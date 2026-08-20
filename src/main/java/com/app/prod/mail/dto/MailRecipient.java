package com.app.prod.mail.dto;

import java.util.UUID;

public record MailRecipient(
        UUID userId,
        String email
) {
}
