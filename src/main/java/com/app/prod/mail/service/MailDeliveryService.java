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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailDeliveryService {

    private final EmailDeliveryRepository emailDeliveryRepository;
    private final MailSender mailSender;
    private final Clock clock;

    /**
     * Safe to call again with the same type and reference: whoever already got the mail is skipped,
     * and whoever it failed for is tried again on the delivery row they already have. That is what
     * lets a job that sends mail be retried.
     * <p>
     * Called outside a transaction, every delivery is recorded the moment it happens.
     *
     * @return how many recipients still have not got it
     */
    public int send(EmailDeliveryType type, UUID referenceId, List<MailRecipient> recipients, MailContent content) {
        if (recipients.isEmpty()) {
            log.info("Delivery {} {} has no recipients, nothing to send", type, referenceId);
            return 0;
        }

        List<EmailDeliveryRecord> deliveries = deliveriesFor(type, referenceId, recipients);

        int sent = 0;
        for (EmailDeliveryRecord delivery : deliveries) {
            if (sendOne(delivery, content)) {
                sent++;
            }
        }

        int failed = deliveries.size() - sent;
        log.info("Delivery {} {} finished: sent {}/{}, {} had it already", type, referenceId, sent, deliveries.size(), recipients.size() - deliveries.size());
        return failed;
    }

    /** The rows still to be sent: earlier attempts that did not go through, plus new ones for whoever has none. */
    private List<EmailDeliveryRecord> deliveriesFor(EmailDeliveryType type, UUID referenceId, List<MailRecipient> recipients) {
        Map<String, EmailDeliveryRecord> earlier = emailDeliveryRepository.findByReference(type, referenceId).stream()
                .collect(Collectors.toMap(MailDeliveryService::recipientKey, Function.identity(), (first, second) -> first));

        List<EmailDeliveryRecord> toSend = new ArrayList<>();
        List<MailRecipient> fresh = new ArrayList<>();
        for (MailRecipient recipient : recipients) {
            EmailDeliveryRecord delivery = earlier.get(recipientKey(recipient.userId(), recipient.email()));
            if (delivery == null) {
                fresh.add(recipient);
            } else if (!EmailDeliveryStatus.SENT.name().equals(delivery.getStatus())) {
                // an account may have changed its address since the last attempt
                delivery.setEmail(recipient.email());
                toSend.add(delivery);
            }
        }

        List<EmailDeliveryRecord> created = toPendingRecords(type, referenceId, fresh);
        emailDeliveryRepository.insertMany(created);
        toSend.addAll(created);
        return toSend;
    }

    /** An account is one recipient whatever its address; a guest is nothing but the address. */
    private static String recipientKey(UUID userId, String email) {
        return userId != null ? "user:" + userId : "email:" + email.toLowerCase(Locale.ROOT);
    }

    private static String recipientKey(EmailDeliveryRecord delivery) {
        return recipientKey(delivery.getUserId(), delivery.getEmail());
    }

    private boolean sendOne(EmailDeliveryRecord delivery, MailContent content) {
        try {
            mailSender.send(new MailMessage(delivery.getEmail(), content.subject(), content.body()));
            emailDeliveryRepository.markSent(delivery.getId(), delivery.getEmail(), LocalDateTime.now(clock));
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
