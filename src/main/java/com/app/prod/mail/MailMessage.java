package com.app.prod.mail;

public record MailMessage(
        String to,
        String subject,
        String body
) {
}
