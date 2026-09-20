package com.app.prod.event.service;

import com.app.prod.event.device.DeviceSignals;
import com.app.prod.event.dto.CreateEventRequest;
import com.app.prod.event.dto.EventResponse;
import com.app.prod.event.enums.EventMemberRole;
import com.app.prod.event.enums.EventMemberStatus;
import com.app.prod.event.mappers.EventMapper;
import com.app.prod.event.repository.EventMemberMetadataRepository;
import com.app.prod.event.repository.EventMemberRepository;
import com.app.prod.event.repository.EventRepository;
import com.app.prod.event.web.EventCaller;
import com.app.prod.event.web.EventScope;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.EventMemberRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PublicEventService {

    private final EventRepository eventRepository;
    private final EventMemberRepository eventMemberRepository;
    private final EventMemberMetadataRepository eventMemberMetadataRepository;
    private final EventSessionService eventSessionService;
    private final ReturnCodes returnCodes;
    private final Clock clock;

    public EventResponse getEvent(String slug, Optional<EventScope> scope) {
        EventResponse event = eventRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.EVENT_NOT_FOUND)));

        return scope.flatMap(member -> eventMemberRepository.findById(member.memberId()))
                .map(member -> event.withMe(EventMapper.toResponse(member)))
                .orElse(event);
    }

    @Transactional
    public CreatedEvent createEvent(CreateEventRequest request, EventCaller caller, DeviceSignals device) {
        validate(request, caller);

        var now = LocalDateTime.now(clock);
        var eventId = UUID.randomUUID();
        String slug = EventSlug.generate();
        eventRepository.insertOne(EventMapper.fromRequestToRecord(request, eventId, slug, now));

        ReturnCodes.IssuedCode code = caller.isGuest() ? returnCodes.issue() : null;
        EventMemberRecord host = caller.uniteUser()
                .map(user -> EventMapper.uniteMember(eventId, user, EventMemberRole.HOST, EventMemberStatus.GOING, now))
                .orElseGet(() -> EventMapper.guestMember(eventId, request.displayName(), code.hash(), EventMemberRole.HOST, EventMemberStatus.GOING, now));
        eventMemberRepository.insertOne(host);
        eventMemberMetadataRepository.save(host.getId(), device, now);

        String sessionToken = eventSessionService.open(host.getId(), now);

        log.info("Created event {} hosted by {}", eventId, caller.isGuest() ? "a guest" : "user " + host.getUserId());
        return new CreatedEvent(slug, code != null ? code.code() : null, sessionToken);
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
}
