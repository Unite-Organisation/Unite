package com.app.prod.event.service;

import com.app.prod.event.enums.EventMemberStatus;
import com.app.prod.event.repository.EventMemberRepository;
import com.app.prod.event.repository.EventRepository;
import com.app.prod.event.repository.EventSlotRepository;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
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
 * Tells the people of an event what happened to its date. Runs after the transaction that changed
 * the event has committed, so what it reads is already the final state.
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

    public void groupFormed(UUID eventId, UUID slotId, LocalDateTime confirmBy) {
        EventRecord event = event(eventId);
        List<MailRecipient> recipients = eventMemberRepository.findGoingVotersRecipients(eventId, slotId);

        Map<String, Object> variables = variables(event, slot(slotId));
        variables.put("confirmBy", format(confirmBy));

        // a group can fall apart and form again, and each time is news - so the reference is the
        // formation (told apart by its walk-out deadline), not just the event
        UUID formation = UUID.nameUUIDFromBytes((eventId + "|" + confirmBy).getBytes(StandardCharsets.UTF_8));
        deliver(EmailDeliveryType.EVENT_GROUP_FORMED, MailTemplate.EVENT_GROUP_FORMED, formation, event, recipients, variables);
    }

    public void confirmed(UUID eventId, UUID slotId) {
        EventRecord event = event(eventId);
        List<MailRecipient> recipients = eventMemberRepository.findRecipientsWithStatus(eventId, EventMemberStatus.GOING);

        deliver(EmailDeliveryType.EVENT_CONFIRMED, MailTemplate.EVENT_CONFIRMED, eventId, event, recipients, variables(event, slot(slotId)));
    }

    public void reducedToOneSlot(UUID eventId, UUID slotId) {
        EventRecord event = event(eventId);
        List<MailRecipient> recipients = eventMemberRepository.findRecipientsWithStatus(eventId, EventMemberStatus.GOING);

        Map<String, Object> variables = variables(event, slot(slotId));
        variables.put("minAttendees", event.getMinAttendees());

        deliver(EmailDeliveryType.EVENT_REDUCED_TO_ONE_SLOT, MailTemplate.EVENT_REDUCED_TO_ONE_SLOT, eventId, event, recipients, variables);
    }

    private void deliver(EmailDeliveryType type, MailTemplate template, UUID referenceId, EventRecord event,
                         List<MailRecipient> recipients, Map<String, Object> variables) {
        MailContent content = mailTemplateProcessor.render(template, new Object[]{event.getName()}, variables);

        log.info("Telling {} members of event {} about {}", recipients.size(), event.getId(), type);
        mailDeliveryService.send(type, referenceId, recipients, content);
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
