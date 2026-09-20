package com.app.prod.event.service;

import com.app.prod.event.dto.OpenSessionRequest;
import com.app.prod.event.enums.EventMemberRole;
import com.app.prod.event.enums.EventMemberStatus;
import com.app.prod.event.mappers.EventMapper;
import com.app.prod.event.repository.EventMemberRepository;
import com.app.prod.event.repository.EventRepository;
import com.app.prod.event.web.EventCaller;
import com.app.prod.event.web.EventScope;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.AuthenticationFailedException;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.exceptions.exceptions.DataAlreadyExistsException;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.AppUserRecord;
import org.jooq.sources.tables.records.EventMemberRecord;
import org.jooq.sources.tables.records.EventRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Who is in an event and with which status. Every change locks the event row first, so capacity checks,
 * waitlist promotion and new names never race each other.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EventMembershipService {

    static final int MAX_FAILED_ATTEMPTS = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final EventRepository eventRepository;
    private final EventMemberRepository eventMemberRepository;
    private final EventSessionService eventSessionService;
    private final ReturnCodes returnCodes;
    private final Clock clock;

    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public OpenedSession openSession(String slug, EventCaller caller, OpenSessionRequest request) {
        EventRecord event = eventRepository.findBySlugForUpdate(slug)
                .orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.EVENT_NOT_FOUND)));
        LocalDateTime now = LocalDateTime.now(clock);

        OpenedSession opened = caller.uniteUser()
                .map(user -> openUniteSession(event.getId(), user, now))
                .orElseGet(() -> openGuestSession(event.getId(), request, now));

        log.info("{} event session for member of event {} ({})", opened.joined() ? "Joined with" : "Reopened", event.getId(), opened.member().origin());
        return opened;
    }

    private OpenedSession openUniteSession(UUID eventId, AppUserRecord user, LocalDateTime now) {
        Optional<EventMemberRecord> existing = eventMemberRepository.findUniteMember(eventId, user.getId());
        EventMemberRecord member = existing.orElseGet(() -> {
            EventMemberRecord created = EventMapper.uniteMember(eventId, user, EventMemberRole.MEMBER, EventMemberStatus.UNDECIDED, now);
            eventMemberRepository.insertOne(created);
            return created;
        });

        String sessionToken = eventSessionService.open(member.getId(), now);
        return new OpenedSession(existing.isEmpty(), EventMapper.toResponse(member), null, sessionToken);
    }

    private OpenedSession openGuestSession(UUID eventId, OpenSessionRequest request, LocalDateTime now) {
        String displayName = Optional.ofNullable(request)
                .map(OpenSessionRequest::displayName)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .orElseThrow(() -> new BadRequestException(AppError.of(Code.VALIDATION_ERROR, "displayName is required without an account")));

        Optional<EventMemberRecord> existing = eventMemberRepository.findGuestByNameForUpdate(eventId, displayName);
        if (existing.isPresent()) {
            EventMemberRecord member = existing.get();
            verifyReturnCode(member, request.returnCode(), now);
            String sessionToken = eventSessionService.open(member.getId(), now);
            return new OpenedSession(false, EventMapper.toResponse(member), null, sessionToken);
        }

        ReturnCodes.IssuedCode code = returnCodes.issue();
        EventMemberRecord member = EventMapper.guestMember(eventId, displayName, code.hash(), EventMemberRole.MEMBER, EventMemberStatus.UNDECIDED, now);
        eventMemberRepository.insertOne(member);

        String sessionToken = eventSessionService.open(member.getId(), now);
        return new OpenedSession(true, EventMapper.toResponse(member), code.code(), sessionToken);
    }

    private void verifyReturnCode(EventMemberRecord member, String returnCode, LocalDateTime now) {
        if (member.getLockedUntil() != null && member.getLockedUntil().isAfter(now)) {
            throw new AuthenticationFailedException(AppError.of(Code.EVENT_MEMBER_LOCKED, String.format("Try again after %s", member.getLockedUntil())));
        }

        // no code: the name belongs to someone - the client asks "is that you?" or for another name
        if (returnCode == null) {
            throw new DataAlreadyExistsException(AppError.of(Code.EVENT_MEMBER_NAME_TAKEN));
        }

        if (!returnCodes.matches(returnCode, member.getPinHash())) {
            int failedAttempts = member.getFailedAttempts() + 1;
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                member.setFailedAttempts(0);
                member.setLockedUntil(now.plus(LOCK_DURATION));
            } else {
                member.setFailedAttempts(failedAttempts);
            }
            eventMemberRepository.update(member);

            log.info("Wrong return code for member {} of event {}, attempt {}", member.getId(), member.getEventId(), failedAttempts);
            throw new AuthenticationFailedException(AppError.of(Code.EVENT_RETURN_CODE_INVALID));
        }

        if (member.getFailedAttempts() > 0 || member.getLockedUntil() != null) {
            member.setFailedAttempts(0);
            member.setLockedUntil(null);
            eventMemberRepository.update(member);
        }
    }

    @Transactional
    public EventMemberStatus changeAttendance(EventScope scope, EventMemberStatus requested) {
        if (requested != EventMemberStatus.GOING && requested != EventMemberStatus.NOT_GOING) {
            throw new BadRequestException(AppError.of(Code.VALIDATION_ERROR, String.format("Attendance can be %s or %s", EventMemberStatus.GOING, EventMemberStatus.NOT_GOING)));
        }

        EventRecord event = eventRepository.findForUpdate(scope.eventId())
                .orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.EVENT_NOT_FOUND)));
        EventMemberRecord member = eventMemberRepository.findById(scope.memberId())
                .orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.EVENT_NOT_FOUND)));
        EventMemberStatus current = EventMemberStatus.valueOf(member.getStatus());
        LocalDateTime now = LocalDateTime.now(clock);

        if (requested == EventMemberStatus.GOING) {
            if (current == EventMemberStatus.GOING || current == EventMemberStatus.WAITLIST) {
                return current;
            }
            EventMemberStatus next = seatFor(event);
            setStatus(member, next, now);
            return next;
        }

        if (current == EventMemberStatus.NOT_GOING) {
            return current;
        }
        setStatus(member, EventMemberStatus.NOT_GOING, now);
        if (current == EventMemberStatus.GOING) {
            promoteFromWaitlist(event, now);
        }
        return EventMemberStatus.NOT_GOING;
    }

    private EventMemberStatus seatFor(EventRecord event) {
        Integer maxAttendees = event.getMaxAttendees();
        if (maxAttendees == null || eventMemberRepository.countWithStatus(event.getId(), EventMemberStatus.GOING) < maxAttendees) {
            return EventMemberStatus.GOING;
        }
        if (Boolean.TRUE.equals(event.getWaitlistEnabled())) {
            return EventMemberStatus.WAITLIST;
        }
        throw new BadRequestException(AppError.of(Code.EVENT_MAX_ATTENDEES));
    }

    private void promoteFromWaitlist(EventRecord event, LocalDateTime now) {
        eventMemberRepository.findFirstWaitlisted(event.getId()).ifPresent(next -> {
            setStatus(next, EventMemberStatus.GOING, now);
            log.info("Promoted member {} from the waitlist of event {}", next.getId(), event.getId());
        });
    }

    private void setStatus(EventMemberRecord member, EventMemberStatus status, LocalDateTime now) {
        member.setStatus(status.name());
        member.setStatusChangedAt(now);
        eventMemberRepository.update(member);
    }
}
