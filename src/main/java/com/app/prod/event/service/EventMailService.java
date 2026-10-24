package com.app.prod.event.service;

import com.app.prod.event.enums.EventMemberStatus;
import com.app.prod.event.repository.EventMemberRepository;
import com.app.prod.event.repository.EventRepository;
import com.app.prod.event.repository.EventSlotRepository;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.exceptions.exceptions.IllegalApplicationStateException;
import com.app.prod.exceptions.exceptions.InternalConnectionException;
import com.app.prod.job.jobs.EventMailJob;
import com.app.prod.mail.config.MailProperties;
import com.app.prod.mail.dto.MailContent;
import com.app.prod.mail.dto.MailRecipient;
import com.app.prod.mail.enums.EmailDeliveryType;
import com.app.prod.mail.service.MailDeliveryService;
import com.app.prod.mail.template.MailTemplate;
import com.app.prod.mail.template.MailTemplateProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.EventRecord;
import org.jooq.sources.tables.records.EventSlotRecord;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Tells the people of an event what happened to its date. Runs from an {@link EventMailJob} drained
 * after the change committed, so it reads the event as it is by then - and it may run more than
 * once for the same change, which {@link MailDeliveryService#send} makes harmless.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventMailService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final EventRepository eventRepository;
    private final EventSlotRepository eventSlotRepository;
    private final EventMemberRepository eventMemberRepository;
    private final MailDeliveryService mailDeliveryService;
    private final MailTemplateProcessor mailTemplateProcessor;
    private final MailProperties mailProperties;

    /** @param groupConfirmBy see {@link EventMailJob} */
    public void send(EmailDeliveryType type, UUID eventId, UUID slotId, LocalDateTime groupConfirmBy) {
        // the job outlives what it was booked for, like the deadline job does
        if (eventRepository.findById(eventId).isEmpty()) {
            log.info("Event {} is gone, its {} mail with it", eventId, type);
            return;
        }

        switch (type) {
            case EVENT_GROUP_FORMED -> groupFormed(eventId, slotId, groupConfirmBy);
            case EVENT_GROUP_BROKE_UP -> groupBrokeUp(eventId, slotId, groupConfirmBy);
            case EVENT_CONFIRMED -> confirmed(eventId, slotId);
            case EVENT_REDUCED_TO_ONE_SLOT -> reducedToOneSlot(eventId, slotId, groupConfirmBy);
            default -> throw new IllegalApplicationStateException(AppError.of(Code.MAIL_SENDING_FAILED,
                    String.format("%s is not a mail about an event", type)));
        }
    }

    private void groupFormed(UUID eventId, UUID slotId, LocalDateTime confirmBy) {
        EventRecord event = event(eventId);
        List<MailRecipient> recipients = eventMemberRepository.findGoingVotersRecipients(eventId, slotId);

        Map<String, Object> variables = variables(event, slot(slotId));
        variables.put("confirmBy", format(confirmBy));

        deliver(EmailDeliveryType.EVENT_GROUP_FORMED, MailTemplate.EVENT_GROUP_FORMED, formation(eventId, confirmBy), event, recipients, variables);
    }

    private void groupBrokeUp(UUID eventId, UUID slotId, LocalDateTime confirmBy) {
        EventRecord event = event(eventId);
        List<MailRecipient> recipients = eventMemberRepository.findGoingVotersRecipients(eventId, slotId);

        Map<String, Object> variables = variables(event, slot(slotId));
        variables.put("votingDeadline", format(event.getVotingDeadline()));

        deliver(EmailDeliveryType.EVENT_GROUP_BROKE_UP, MailTemplate.EVENT_GROUP_BROKE_UP, formation(eventId, confirmBy), event, recipients, variables);
    }

    private void confirmed(UUID eventId, UUID slotId) {
        EventRecord event = event(eventId);
        List<MailRecipient> recipients = eventMemberRepository.findRecipientsWithStatus(eventId, EventMemberStatus.GOING);

        deliver(EmailDeliveryType.EVENT_CONFIRMED, MailTemplate.EVENT_CONFIRMED, eventId, event, recipients, variables(event, slot(slotId)));
    }

    private void reducedToOneSlot(UUID eventId, UUID slotId, LocalDateTime brokenUpGroupConfirmBy) {
        EventRecord event = event(eventId);
        List<MailRecipient> recipients = eventMemberRepository.findRecipientsWithStatus(eventId, EventMemberStatus.GOING);
        boolean afterBreakUp = brokenUpGroupConfirmBy != null;

        Map<String, Object> variables = variables(event, slot(slotId));
        variables.put("minAttendees", event.getMinAttendees());
        variables.put("afterBreakUp", afterBreakUp);

        // the deadline reduces an event once, a group falling apart on the last date can do it again
        // and again - each of those is news, and a reference of its own
        UUID reference = afterBreakUp ? formation(eventId, brokenUpGroupConfirmBy) : eventId;
        deliver(EmailDeliveryType.EVENT_REDUCED_TO_ONE_SLOT, MailTemplate.EVENT_REDUCED_TO_ONE_SLOT, reference, event, recipients, variables);
    }

    private static UUID formation(UUID eventId, LocalDateTime confirmBy) {
        return UUID.nameUUIDFromBytes((eventId + "|" + confirmBy).getBytes(StandardCharsets.UTF_8));
    }

    private void deliver(EmailDeliveryType type, MailTemplate template, UUID referenceId, EventRecord event,
                         List<MailRecipient> recipients, Map<String, Object> variables) {
        MailContent content = mailTemplateProcessor.render(template, new Object[]{event.getName()}, variables);

        log.info("Telling {} members of event {} about {}", recipients.size(), event.getId(), type);
        int notDelivered = mailDeliveryService.send(type, referenceId, recipients, content);

        // failing the job is what brings it back; the next attempt only writes to whoever is left
        if (notDelivered > 0) {
            throw new InternalConnectionException(AppError.of(Code.MAIL_SENDING_FAILED,
                    String.format("%s of %s mails about event %s did not go out", notDelivered, recipients.size(), event.getId())));
        }
    }

    private Map<String, Object> variables(EventRecord event, EventSlotRecord slot) {
        Locale locale = mailProperties.getLocale();
        Map<String, Object> variables = new HashMap<>();

        variables.put("appName", mailProperties.getFromName());
        variables.put("eventName", event.getName());
        variables.put("eventLink", eventLink(event.getPublicSlug()));
        variables.put("when", when(slot, locale));
        variables.put("location", event.getLocationName());
        variables.put("onlineUrl", event.getOnlineUrl());
        return variables;
    }

    private String when(EventSlotRecord slot, Locale locale) {
        String start = format(slot.getStartDateTime(), locale);
        LocalDateTime end = slot.getEndDateTime();

        if (end == null) {
            return start;
        }
        return slot.getStartDateTime().toLocalDate().equals(end.toLocalDate())
                ? start + " – " + TIME.format(end)
                : start + " – " + format(end, locale);
    }

    private String format(LocalDateTime dateTime) {
        return format(dateTime, mailProperties.getLocale());
    }

    private static String format(LocalDateTime dateTime, Locale locale) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT)
                .withLocale(locale)
                .format(dateTime);
    }

    private String eventLink(String slug) {
        String base = mailProperties.getEventLinkBaseUrl();
        return (base.endsWith("/") ? base : base + "/") + slug;
    }

    private EventRecord event(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.EVENT_NOT_FOUND)));
    }

    private EventSlotRecord slot(UUID slotId) {
        return eventSlotRepository.findById(slotId)
                .orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.EVENT_SLOT_NOT_FOUND)));
    }
}
