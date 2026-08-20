package com.app.prod.mail;

import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.InternalConnectionException;
import com.app.prod.mail.config.MailProperties;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmtpMailSender implements MailSender {

    private final JavaMailSender javaMailSender;
    private final MailProperties mailProperties;

    @Override
    public void send(MailMessage message) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());

            helper.setFrom(new InternetAddress(mailProperties.getFrom(), mailProperties.getFromName(), StandardCharsets.UTF_8.name()));
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(message.body(), true);

            javaMailSender.send(mimeMessage);
            log.debug("Mail sent to {} subject={}", message.to(), message.subject());
        } catch (Exception e) {
            throw new InternalConnectionException(AppError.of(
                    Code.MAIL_SENDING_FAILED,
                    String.format("Mail to %s could not be sent: %s", message.to(), e.getMessage())
            ));
        }
    }
}
