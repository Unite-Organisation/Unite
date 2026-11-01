package com.app.prod.event.service;

import com.app.prod.event.repository.EventMemberRepository;
import com.app.prod.event.repository.EventRepository;
import com.app.prod.event.repository.EventSlotRepository;
import com.app.prod.mail.config.MailProperties;
import com.app.prod.mail.dto.MailContent;
import com.app.prod.mail.dto.MailRecipient;
import com.app.prod.mail.enums.EmailDeliveryType;
import com.app.prod.mail.service.MailDeliveryService;
import com.app.prod.mail.template.MailTemplate;
import com.app.prod.mail.template.MailTemplateProcessor;
import org.jooq.sources.tables.records.EventMemberRecord;
import org.jooq.sources.tables.records.EventRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EventMailServiceTest {

    private final EventRepository eventRepository = mock(EventRepository.class);
    private final EventMemberRepository eventMemberRepository = mock(EventMemberRepository.class);
    private final MailDeliveryService mailDeliveryService = mock(MailDeliveryService.class);
    private final MailTemplateProcessor mailTemplateProcessor = mock(MailTemplateProcessor.class);
    private final MailProperties mailProperties = new MailProperties();

    private final List<MailRecipient> recipients = List.of(mock(MailRecipient.class));
    private final UUID memberId = UUID.randomUUID();

    private EventMailService service;

    @BeforeEach
    void setUp() {
        service = new EventMailService(eventRepository, mock(EventSlotRepository.class), eventMemberRepository,
                mailDeliveryService, mailTemplateProcessor, mailProperties);

        UUID eventId = UUID.randomUUID();
        EventRecord event = new EventRecord();
        event.setId(eventId);
        event.setName("Planszówki");
        event.setPublicSlug("planszowki");

        EventMemberRecord member = new EventMemberRecord();
        member.setId(memberId);
        member.setEventId(eventId);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventMemberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(eventMemberRepository.findMemberRecipient(memberId)).thenReturn(recipients);
    }

    @Test
    void sendsEventMailByDefault() {
        MailContent content = mock(MailContent.class);
        when(mailTemplateProcessor.render(eq(MailTemplate.EVENT_MEMBER_JOINED), any(Object[].class), any())).thenReturn(content);

        service.sendReturnCode(memberId, "ABC123");

        verify(mailDeliveryService).send(EmailDeliveryType.EVENT_MEMBER_CREATED, memberId, recipients, content);
    }

    @Test
    void sendsNothingWhenEventMailsAreTurnedOff() {
        mailProperties.setEventNotificationsEnabled(false);

        service.sendReturnCode(memberId, "ABC123");

        verifyNoInteractions(mailTemplateProcessor);
        verify(mailDeliveryService, never()).send(any(), any(), any(), any());
    }

    @Test
    void devProfileTurnsEventMailsOff() throws IOException {
        var sources = new YamlPropertySourceLoader().load("dev", new ClassPathResource("application-dev.yml"));

        MailProperties dev = new Binder(ConfigurationPropertySources.from(sources)).bind("mail", MailProperties.class).get();

        assertThat(dev.isEventNotificationsEnabled()).isFalse();
    }
}
