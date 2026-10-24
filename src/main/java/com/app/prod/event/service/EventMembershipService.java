package com.app.prod.event.service;

import com.app.prod.event.device.DeviceSignals;
import com.app.prod.event.device.MembershipScorer;
import com.app.prod.event.dto.*;
import com.app.prod.event.enums.EventMemberRole;
import com.app.prod.event.enums.EventMemberStatus;
import com.app.prod.event.enums.EventStatus;
import com.app.prod.event.enums.SchedulingMode;
import com.app.prod.event.mappers.EventMapper;
import com.app.prod.event.repository.EventMemberMetadataRepository;
import com.app.prod.event.repository.EventMemberRepository;
import com.app.prod.event.repository.EventRepository;
import com.app.prod.event.repository.EventSlotVoteRepository;
import com.app.prod.event.web.EventCaller;
import com.app.prod.event.web.EventScope;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.AuthenticationFailedException;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.exceptions.exceptions.DataAlreadyExistsException;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.exceptions.exceptions.IllegalApplicationStateException;
import com.app.prod.exceptions.exceptions.UnauthorizedDataAccessException;
import com.app.prod.utils.Pagination;
import com.app.prod.utils.filters.EventMemberFilter;
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
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventMembershipService {

    static final int MAX_FAILED_ATTEMPTS = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final EventRepository eventRepository;
    private final EventMemberRepository eventMemberRepository;
    private final EventMemberMetadataRepository eventMemberMetadataRepository;
    private final MembershipScorer membershipScorer;
    private final EventVotingService eventVotingService;
    private final EventLifecycleService eventLifecycleService;
    private final EventSessionService eventSessionService;
    private final ReturnCodes returnCodes;
    private final Clock clock;
    private final EventSlotVoteRepository eventSlotVoteRepository;

    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public OpenedSession openSession(String slug, EventCaller caller, OpenSessionRequest request, DeviceSignals device) {
        EventRecord event = eventRepository.findBySlugForUpdate(slug)
                .orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.EVENT_NOT_FOUND)));
        LocalDateTime now = LocalDateTime.now(clock);

        EventMemberStatus newcomer = Boolean.TRUE.equals(event.getRequiresApproval())
                ? EventMemberStatus.PENDING_APPROVAL
                : EventMemberStatus.UNDECIDED;

        OpenedSession opened = caller.uniteUser()
                .map(user -> openUniteSession(event.getId(), user, newcomer, now, device))
                .orElseGet(() -> openGuestSession(event.getId(), request, newcomer, now, device));

        applyVotesIfChoosingDate(event, opened, request, now);

        log.info("{} event session for member of event {} ({})", opened.joined() ? "Joined with" : "Reopened", event.getId(), opened.member().origin());
        return opened;
    }

    private void applyVotesIfChoosingDate(EventRecord event, OpenedSession opened, OpenSessionRequest request, LocalDateTime now) {
        if (SchedulingMode.valueOf(event.getSchedulingMode()) != SchedulingMode.POLL) {
            return;
        }

        // once the date is final there is nothing left to choose: the event is an ordinary dated one
        // and people sign up for it, seats handed out by attendance the same as anywhere else
        if (EventStatus.valueOf(event.getStatus()) == EventStatus.CONFIRMED) {
            return;
        }

        List<SlotVoteRequest> votes = request == null ? null : request.votes();
        if (votes == null && !opened.joined()) {
            return;
        }

        eventVotingService.save(event, opened.memberId(), votes, now);
        eventLifecycleService.recheckGroup(event.getId(), now);
    }

    private OpenedSession openUniteSession(UUID eventId, AppUserRecord user, EventMemberStatus newcomer, LocalDateTime now, DeviceSignals device) {
        Optional<EventMemberRecord> existing = eventMemberRepository.findUniteMember(eventId, user.getId());
        EventMemberRecord member = existing.orElseGet(() -> {
            EventMemberRecord created = EventMapper.uniteMember(eventId, user, EventMemberRole.MEMBER, newcomer, now);
            eventMemberRepository.insertOne(created);
            return created;
        });

        eventMemberMetadataRepository.save(member.getId(), device, now);
        String sessionToken = eventSessionService.open(member.getId(), now);
        return new OpenedSession(existing.isEmpty(), member.getId(), EventMapper.toResponse(member), null, sessionToken);
    }

    private OpenedSession openGuestSession(UUID eventId, OpenSessionRequest request, EventMemberStatus newcomer, LocalDateTime now, DeviceSignals device) {
        String displayName = Optional.ofNullable(request)
                .map(OpenSessionRequest::displayName)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .orElseThrow(() -> new BadRequestException(AppError.of(Code.VALIDATION_ERROR, "displayName is required without an account")));

        Optional<EventMemberRecord> existing = eventMemberRepository.findGuestByNameForUpdate(eventId, displayName);
        if (existing.isPresent()) {
            EventMemberRecord member = existing.get();
            verifyReturnCode(member, request.returnCode(), now);
            rememberEmailIfMissing(member, request.email());
            eventMemberMetadataRepository.save(member.getId(), device, now);
            String sessionToken = eventSessionService.open(member.getId(), now);
            return new OpenedSession(false, member.getId(), EventMapper.toResponse(member), null, sessionToken);
        }

        String email = Optional.ofNullable(request.email())
                .map(String::trim)
                .filter(address -> !address.isEmpty())
                .orElseThrow(() -> new BadRequestException(AppError.of(Code.VALIDATION_ERROR, "email is required the first time a guest joins")));

        ReturnCodes.IssuedCode code = returnCodes.issue();
        EventMemberRecord member = EventMapper.guestMember(eventId, displayName, email, code.hash(), EventMemberRole.MEMBER, newcomer, now);
        eventMemberRepository.insertOne(member);
        eventMemberMetadataRepository.save(member.getId(), device, now);

        String sessionToken = eventSessionService.open(member.getId(), now);
        return new OpenedSession(true, member.getId(), EventMapper.toResponse(member), code.code(), sessionToken);
    }

    /** Members who joined before there was an email to give get to add it the next time they come back. */
    private void rememberEmailIfMissing(EventMemberRecord member, String email) {
        if (member.getEmail() != null || email == null || email.isBlank()) {
            return;
        }
        member.setEmail(email.trim());
        eventMemberRepository.update(member);
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

        if (current == EventMemberStatus.PENDING_APPROVAL) {
            throw new UnauthorizedDataAccessException(AppError.of(Code.EVENT_MEMBER_AWAITING_APPROVAL));
        }

        if (isChoosingDate(event)) {
            return changeWhileChoosingDate(event, member, current, requested, now);
        }

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
        eventSlotVoteRepository.replaceForMember(member.getId(), Collections.emptyList(), now);
        return EventMemberStatus.NOT_GOING;
    }

    private EventMemberStatus changeWhileChoosingDate(EventRecord event, EventMemberRecord member,
                                                      EventMemberStatus current, EventMemberStatus requested,
                                                      LocalDateTime now) {
        if (current == requested) {
            return current;
        }

        setStatus(member, requested, now);
        eventLifecycleService.recheckGroup(event.getId(), now);
        return requested;
    }

    private static boolean isChoosingDate(EventRecord event) {
        return SchedulingMode.valueOf(event.getSchedulingMode()) == SchedulingMode.POLL
                && EventStatus.valueOf(event.getStatus()) != EventStatus.CONFIRMED;
    }

    /**
     * Lets in the next members of the queue. The host chooses how many, never who goes first: the
     * ids have to be the head of the queue, because being let in is what hands out the seats.
     */
    @Transactional
    public void approveMembers(EventScope scope, List<UUID> memberIds) {
        requireHost(scope);
        EventRecord event = eventRepository.findForUpdate(scope.eventId())
                .orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.EVENT_NOT_FOUND)));
        LocalDateTime now = LocalDateTime.now(clock);

        Set<UUID> requested = Set.copyOf(memberIds);
        List<UUID> queue = eventMemberRepository.findApprovalQueue(event.getId());
        if (!new HashSet<>(queue).containsAll(requested)) {
            throw new BadRequestException(AppError.of(Code.EVENT_MEMBER_NOT_AWAITING_APPROVAL));
        }
        if (!Set.copyOf(queue.subList(0, requested.size())).equals(requested)) {
            throw new BadRequestException(AppError.of(Code.EVENT_APPROVAL_OUT_OF_ORDER,
                    String.format("The first %s in the queue have to be approved first", requested.size())));
        }

        boolean choosingDate = isChoosingDate(event);
        for (EventMemberRecord member : eventMemberRepository.findInEvent(event.getId(), requested)) {
            setStatus(member, choosingDate ? EventMemberStatus.GOING : admittedTo(event), now);
        }
        if (choosingDate) {
            eventLifecycleService.recheckGroup(event.getId(), now);
        }

        log.info("Host of event {} approved {} member(s), {} still waiting", event.getId(), requested.size(), queue.size() - requested.size());
    }

    @Transactional
    public void removeMembers(EventScope scope, List<UUID> memberIds) {
        requireHost(scope);
        EventRecord event = eventRepository.findForUpdate(scope.eventId())
                .orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.EVENT_NOT_FOUND)));
        LocalDateTime now = LocalDateTime.now(clock);

        Set<UUID> requested = Set.copyOf(memberIds);
        List<EventMemberRecord> members = eventMemberRepository.findInEvent(event.getId(), requested);
        if (members.size() != requested.size()) {
            throw new EntityNotPresentException(AppError.of(Code.EVENT_MEMBER_NOT_FOUND));
        }
        if (members.stream().anyMatch(member -> EventMemberRole.valueOf(member.getRole()) == EventMemberRole.HOST)) {
            throw new BadRequestException(AppError.of(Code.EVENT_HOST_CANNOT_BE_REMOVED));
        }

        long freedSeats = members.stream()
                .filter(member -> EventMemberStatus.valueOf(member.getStatus()) == EventMemberStatus.GOING)
                .count();
        eventMemberRepository.deleteByIds(requested);

        if (isChoosingDate(event)) {
            eventLifecycleService.recheckGroup(event.getId(), now);
        } else {
            for (long seat = 0; seat < freedSeats; seat++) {
                promoteFromWaitlist(event, now);
            }
        }

        log.info("Host of event {} removed {} member(s)", event.getId(), requested.size());
    }

    private static void requireHost(EventScope scope) {
        if (scope.role() != EventMemberRole.HOST) {
            throw new UnauthorizedDataAccessException(AppError.of(Code.EVENT_HOST_REQUIRED));
        }
    }

    /** A seat if there is one, else the waitlist - and with neither they are in, just without a seat. */
    private EventMemberStatus admittedTo(EventRecord event) {
        if (hasFreeSeat(event)) {
            return EventMemberStatus.GOING;
        }
        return Boolean.TRUE.equals(event.getWaitlistEnabled())
                ? EventMemberStatus.WAITLIST
                : EventMemberStatus.UNDECIDED;
    }

    private boolean hasFreeSeat(EventRecord event) {
        Integer maxAttendees = event.getMaxAttendees();
        return maxAttendees == null || eventMemberRepository.countWithStatus(event.getId(), EventMemberStatus.GOING) < maxAttendees;
    }

    private EventMemberStatus seatFor(EventRecord event) {
        if (hasFreeSeat(event)) {
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

    public List<EventMemberResponse> findMembers(EventMemberFilter filter, EventMembersRequest request, DeviceSignals caller) {
        List<EventMemberRow> members = eventMemberRepository.findMembers(filter);

        if (request.isAwaitingApproval()) {
            members = members.stream()
                    .sorted(Comparator.comparing(EventMemberRow::joinedAt).thenComparing(EventMemberRow::id))
                    .toList();
        } else if (request.isSortedByCallerMembershipProbability()) {
            members = sortedByProbability(members, filter, caller);
        }

        Pagination pagination = filter.pagination();
        return members.stream()
                .skip(pagination.getOffset())
                .limit(pagination.pageSize())
                .map(member -> new EventMemberResponse(
                        member.id(),
                        member.displayName(),
                        member.role() == EventMemberRole.HOST,
                        member.status(),
                        member.preferredSlots(),
                        member.optionalSlots(),
                        member.joinedAt()
                ))
                .toList();
    }

    private List<EventMemberRow> sortedByProbability(List<EventMemberRow> members, EventMemberFilter filter, DeviceSignals caller) {
        UUID eventId = filter.getEventId().value()
                .orElseThrow(() -> new IllegalApplicationStateException(AppError.of(Code.MANDATORY_FILTER_MISSING, "eventId")));
        Map<UUID, List<DeviceSignals>> devices = eventMemberMetadataRepository.findByEvent(eventId);

        return members.stream()
                .sorted(Comparator.comparingInt(
                        (EventMemberRow member) -> membershipScorer.bestScore(caller, devices.getOrDefault(member.id(), List.of()))
                ).reversed())
                .toList();
    }
}
