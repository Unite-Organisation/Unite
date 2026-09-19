package com.app.prod.event.service;

import com.app.prod.event.dto.CreateEventRequest;
import com.app.prod.event.dto.EventResponse;
import com.app.prod.event.enums.EventMemberRole;
import com.app.prod.event.enums.EventMemberStatus;
import com.app.prod.event.mappers.EventMapper;
import com.app.prod.event.repository.EventMemberRepository;
import com.app.prod.event.repository.EventRepository;
import com.app.prod.event.web.EventCaller;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.EventMemberRecord;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PublicEventService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final EventRepository eventRepository;
    private final EventMemberRepository eventMemberRepository;
    private final EventSessionService eventSessionService;
    private final BCryptPasswordEncoder encoder;
    private final Clock clock;

    public EventResponse getEvent(String slug) {
        return eventRepository.findBySlug(slug).orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.EVENT_NOT_FOUND)));
    }

    /**
     * Creates the event with the caller as its host and opens the host's event session. A Unite account
     * is identified by its JWT, a guest by the display name they typed and a return code generated here.
     */
    @Transactional
    public CreatedEvent createEvent(CreateEventRequest request, EventCaller caller) {
        validate(request, caller);

        var now = LocalDateTime.now(clock);
        var eventId = UUID.randomUUID();
        String slug = EventSlug.generate();
        eventRepository.insertOne(EventMapper.fromRequestToRecord(request, eventId, slug, now));

        String returnCode = caller.isGuest() ? newReturnCode() : null;
        EventMemberRecord host = caller.uniteUser()
                .map(user -> EventMapper.uniteMember(eventId, user, EventMemberRole.HOST, EventMemberStatus.GOING, now))
                .orElseGet(() -> EventMapper.guestMember(eventId, request.displayName(), encoder.encode(returnCode), EventMemberRole.HOST, EventMemberStatus.GOING, now));
        eventMemberRepository.insertOne(host);

        String sessionToken = eventSessionService.open(host.getId(), now);

        log.info("Created event {} hosted by {}", eventId, caller.isGuest() ? "a guest" : "user " + host.getUserId());
        return new CreatedEvent(slug, returnCode, sessionToken);
    }

    private static void validate(CreateEventRequest request, EventCaller caller) {
        if (caller.isGuest() && (request.displayName() == null || request.displayName().isBlank())) {
            throw new BadRequestException(AppError.of(Code.VALIDATION_ERROR, "displayName is required when creating an event without an account"));
        }

        if (request.startDate() != null && request.endDate() != null && request.startDate().isAfter(request.endDate())) {
            throw new BadRequestException(AppError.of(Code.INVALID_TIME_PERIOD, String.format("%s is after %s", request.startDate(), request.endDate())));
        }

        if (request.waitlistEnabled() && request.maxAttendees() == null) {
            throw new BadRequestException(AppError.of(Code.VALIDATION_ERROR, "waitlist requires maxAttendees"));
        }
    }

    private static String newReturnCode() {
        return String.format("%04d", RANDOM.nextInt(10_000));
    }
}
