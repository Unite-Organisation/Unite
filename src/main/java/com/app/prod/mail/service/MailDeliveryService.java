package com.app.prod.mail.service;

import com.app.prod.mail.MailMessage;
import com.app.prod.mail.MailSender;
import com.app.prod.mail.dto.MailContent;
import com.app.prod.mail.dto.MailRecipient;
import com.app.prod.mail.enums.EmailDeliveryStatus;
import com.app.prod.mail.enums.EmailDeliveryType;
import com.app.prod.mail.repository.EmailDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.EmailDeliveryRecord;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailDeliveryService {

    private final EmailDeliveryRepository emailDeliveryRepository;
    private final MailSender mailSender;
    private final Clock clock;

    public void send(EmailDeliveryType type, UUID referenceId, List<MailRecipient> recipients, MailContent content) {
        if (recipients.isEmpty()) {
            log.info("Delivery {} {} has no recipients, nothing to send", type, referenceId);
            return;
        }

        List<EmailDeliveryRecord> deliveries = toPendingRecords(type, referenceId, recipients);
        emailDeliveryRepository.insertMany(deliveries);

        int sent = 0;
        for (EmailDeliveryRecord delivery : deliveries) {
            if (sendOne(delivery, content)) {
                sent++;
            }
        }

        log.info("Delivery {} {} finished: sent {}/{}", type, referenceId, sent, deliveries.size());
    }

    private boolean sendOne(EmailDeliveryRecord delivery, MailContent content) {
        try {
            mailSender.send(new MailMessage(delivery.getEmail(), content.subject(), content.body()));
            emailDeliveryRepository.markSent(delivery.getId(), LocalDateTime.now(clock));
            return true;
        } catch (Exception e) {
            log.warn("Mail to {} failed", delivery.getEmail(), e);
            emailDeliveryRepository.markFailed(delivery.getId(), Objects.toString(e.getMessage(), e.getClass().getSimpleName()));
            return false;
        }
    }

    private List<EmailDeliveryRecord> toPendingRecords(
            EmailDeliveryType type,
            UUID referenceId,
            List<MailRecipient> recipients
    ) {
        LocalDateTime now = LocalDateTime.now(clock);

        return recipients.stream()
                .map(recipient -> new EmailDeliveryRecord(
                        UUID.randomUUID(),
                        type.name(),
                        referenceId,
                        recipient.userId(),
                        recipient.email(),
                        EmailDeliveryStatus.PENDING.name(),
                        null,
                        now,
                        null
                ))
                .toList();
    }
}
