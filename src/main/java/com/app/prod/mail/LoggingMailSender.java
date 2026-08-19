package com.app.prod.mail;

import lombok.extern.slf4j.Slf4j;

/**
 * Stand-in until a mail provider is chosen - registering any other {@link MailSender} bean replaces
 * it without touching handlers or delivery tracking. Deliveries are still recorded, so the flow can
 * be exercised end to end before a single real mail is sent.
 */
@Slf4j
public class LoggingMailSender implements MailSender {

    @Override
    public void send(MailMessage message) {
        log.info("MAIL to={} subject={}", message.to(), message.subject());
    }
}
