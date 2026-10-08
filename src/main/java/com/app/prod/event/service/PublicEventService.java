package com.app.prod.event.service;

import com.app.prod.event.device.DeviceSignals;
import com.app.prod.event.dto.CreateEventRequest;
import com.app.prod.event.dto.EventResponse;
import com.app.prod.event.dto.EventRow;
import com.app.prod.event.dto.EventSlotResponse;
import com.app.prod.event.dto.RankedSlot;
import com.app.prod.event.dto.SlotRequest;
import com.app.prod.event.dto.SlotVote;
import com.app.prod.event.dto.SlotVoterResponse;
import com.app.prod.event.enums.EventMemberRole;
import com.app.prod.event.enums.EventMemberStatus;
import com.app.prod.event.enums.SchedulingMode;
import com.app.prod.event.enums.SlotPreference;
import com.app.prod.event.mappers.EventMapper;
import com.app.prod.event.repository.EventMemberMetadataRepository;
import com.app.prod.event.repository.EventMemberRepository;
import com.app.prod.event.repository.EventRepository;
import com.app.prod.event.repository.EventSlotRepository;
import com.app.prod.event.repository.EventSlotVoteRepository;
import com.app.prod.event.web.EventCaller;
import com.app.prod.event.web.EventScope;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.exceptions.exceptions.UnauthorizedDataAccessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.EventMemberRecord;
import org.jooq.sources.tables.records.EventSlotRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PublicEventService {

    private static final int MIN_SLOTS = 2;
    private static final int MAX_SLOTS = 6;

    private final EventRepository eventRepository;
    private final EventMemberRepository eventMemberRepository;
    private final EventMemberMetadataRepository eventMemberMetadataRepository;
    private final EventSlotRepository eventSlotRepository;
    private final EventSlotVoteRepository eventSlotVoteRepository;
    private final EventLifecycleService eventLifecycleService;
    private final EventSessionService eventSessionService;
    private final ReturnCodes returnCodes;
    private final Clock clock;

    public EventResponse getEvent(String slug, Optional<EventScope> scope) {
        EventRow row = eventRepository.findRowBySlug(slug)
                .orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.EVENT_NOT_FOUND)));

        boolean choosingDate = row.schedulingMode() == SchedulingMode.POLL;
        Optional<EventMemberRecord> caller = scope.flatMap(member -> eventMemberRepository.findById(member.memberId()));

        return new EventResponse(
                row.slug(),
                row.name(),
                row.description(),
                row.startDate(),
                row.endDate(),
                row.locationName(),
                row.onlineUrl(),
                row.maxAttendees(),
                row.waitlistEnabled(),
                row.schedulingMode(),
                row.status(),
                row.minAttendees(),
                row.votingDeadline(),
                row.selectedSlotId(),
                choosingDate ? tiles(row) : List.of(),
                row.goingCount(),
                row.createdAt(),
                caller.map(EventMapper::toResponse).orElse(null),
                choosingDate
                        ? caller.map(member -> eventSlotVoteRepository.findByMember(member.getId())).orElse(List.of())
                        : List.of()
        );
    }

    public List<SlotVoterResponse> getSlotVoters(String slug, UUID slotId) {
        EventRow row = eventRepository.findRowBySlug(slug)
                .orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.EVENT_NOT_FOUND)));

        if (!eventSlotRepository.allBelongToEvent(row.id(), List.of(slotId))) {
            throw new EntityNotPresentException(AppError.of(Code.EVENT_SLOT_NOT_FOUND));
        }

        return eventMemberRepository.findGoingVotersWithPreference(row.id(), slotId);
    }

    @Transactional
    public CreatedEvent createEvent(CreateEventRequest request, EventCaller caller, DeviceSignals device) {
        LocalDateTime now = LocalDateTime.now(clock);
        validate(request, caller, now);

        UUID eventId = UUID.randomUUID();
        String slug = EventSlug.generate();
        eventRepository.insertOne(EventMapper.fromRequestToRecord(request, eventId, slug, now));

        List<EventSlotRecord> slots = request.slotsOrEmpty().stream()
                .map(slot -> EventMapper.slot(eventId, slot, now))
                .toList();
        slots.forEach(eventSlotRepository::insertOne);

        ReturnCodes.IssuedCode code = caller.isGuest() ? returnCodes.issue() : null;
        EventMemberRecord host = caller.uniteUser()
                .map(user -> EventMapper.uniteMember(eventId, user, EventMemberRole.HOST, EventMemberStatus.GOING, now))
                .orElseGet(() -> EventMapper.guestMember(eventId, request.displayName(), code.hash(), EventMemberRole.HOST, EventMemberStatus.GOING, now));
        eventMemberRepository.insertOne(host);
        eventMemberMetadataRepository.save(host.getId(), device, now);

        if (request.mode() == SchedulingMode.POLL) {
            eventSlotVoteRepository.replaceForMember(
                    host.getId(),
                    slots.stream()
                        .map(slot -> new SlotVote(slot.getId(), SlotPreference.PREFERRED))
                        .toList(),
                    now
            );
            eventLifecycleService.scheduleDeadlineCheck(eventId, request.votingDeadline());
            eventLifecycleService.recheckGroup(eventId, now);
        }

        String sessionToken = eventSessionService.open(host.getId(), now);

        log.info("Created {} event {} hosted by {}", request.mode(), eventId, caller.isGuest() ? "a guest" : "user " + host.getUserId());
        return new CreatedEvent(slug, code != null ? code.code() : null, sessionToken);
    }

    @Transactional
    public void confirmAsHost(EventScope scope) {
        if (scope.role() != EventMemberRole.HOST) {
            throw new UnauthorizedDataAccessException(AppError.of(Code.EVENT_HOST_REQUIRED));
        }
        eventLifecycleService.startEventNow(scope.eventId(), LocalDateTime.now(clock));
    }

    private List<EventSlotResponse> tiles(EventRow row) {
        Map<UUID, EventSlotRecord> slots = eventSlotRepository.findByEvent(row.id()).stream()
                .collect(Collectors.toMap(EventSlotRecord::getId, Function.identity()));

        return SlotRanking.rank(eventSlotRepository.tallies(row.id()), row.minAttendees()).stream()
                .map(ranked -> tile(ranked, slots.get(ranked.slotId()), row))
                .toList();
    }

    private static EventSlotResponse tile(RankedSlot ranked, EventSlotRecord slot, EventRow row) {
        return new EventSlotResponse(
                ranked.slotId(),
                slot.getStartDateTime(),
                slot.getEndDateTime(),
                ranked.tally().preferred(),
                ranked.tally().ifNeeded(),
                ranked.chance(),
                stillOpen(row, ranked.slotId())
        );
    }

    private static boolean stillOpen(EventRow row, UUID slotId) {
        return switch (row.status()) {
            case COLLECTING_VOTES -> true;
            case ONE_SLOT_LEFT -> slotId.equals(row.selectedSlotId());
            case GROUP_FORMED, CONFIRMED -> false;
        };
    }

    private static void validate(CreateEventRequest request, EventCaller caller, LocalDateTime now) {
        if (caller.isGuest() && (request.displayName() == null || request.displayName().isBlank())) {
            throw new BadRequestException(AppError.of(Code.VALIDATION_ERROR, "displayName is required when creating an event without an account"));
        }

        if (request.waitlistEnabled() && request.maxAttendees() == null) {
            throw new BadRequestException(AppError.of(Code.VALIDATION_ERROR, "waitlist requires maxAttendees"));
        }

        if (request.mode() == SchedulingMode.POLL) {
            validateChoosingDate(request, now);
        } else {
            validateDated(request);
        }
    }

    private static void validateDated(CreateEventRequest request) {
        if (!request.slotsOrEmpty().isEmpty() || request.votingDeadline() != null || request.minAttendees() != null) {
            throw new BadRequestException(AppError.of(Code.VALIDATION_ERROR,
                    "An event with a date does not propose dates, a deadline or a threshold"));
        }

        if (request.startDate() != null && request.endDate() != null && request.startDate().isAfter(request.endDate())) {
            throw new BadRequestException(AppError.of(Code.INVALID_TIME_PERIOD,
                    String.format("%s is after %s", request.startDate(), request.endDate())));
        }
    }

    private static void validateChoosingDate(CreateEventRequest request, LocalDateTime now) {
        if (request.startDate() != null || request.endDate() != null) {
            throw new BadRequestException(AppError.of(Code.VALIDATION_ERROR,
                    "The date is what the group decides, so it cannot be given up front"));
        }

        if (request.minAttendees() == null || request.votingDeadline() == null) {
            throw new BadRequestException(AppError.of(Code.VALIDATION_ERROR,
                    "An event without a date needs a threshold and a deadline"));
        }

        if (!request.votingDeadline().isAfter(now)) {
            throw new BadRequestException(AppError.of(Code.INVALID_TIME_PERIOD, "The deadline has already passed"));
        }

        if (request.maxAttendees() != null && request.minAttendees() > request.maxAttendees()) {
            throw new BadRequestException(AppError.of(Code.VALIDATION_ERROR,
                    "A group that cannot reach its own threshold would collect answers for nothing"));
        }

        validateSlots(request);
    }

    private static void validateSlots(CreateEventRequest request) {
        List<SlotRequest> slots = request.slotsOrEmpty();
        if (slots.size() < MIN_SLOTS || slots.size() > MAX_SLOTS) {
            throw new BadRequestException(AppError.of(Code.VALIDATION_ERROR,
                    String.format("Propose between %s and %s dates", MIN_SLOTS, MAX_SLOTS)));
        }

        Set<LocalDateTime> starts = new HashSet<>();
        for (SlotRequest slot : slots) {
            if (!starts.add(slot.startDate())) {
                throw new BadRequestException(AppError.of(Code.VALIDATION_ERROR, "The same date was proposed twice"));
            }

            if (slot.endDate() != null && slot.startDate().isAfter(slot.endDate())) {
                throw new BadRequestException(AppError.of(Code.INVALID_TIME_PERIOD,
                        String.format("%s is after %s", slot.startDate(), slot.endDate())));
            }

            if (!slot.startDate().isAfter(request.votingDeadline())) {
                throw new BadRequestException(AppError.of(Code.INVALID_TIME_PERIOD,
                        "A proposed date cannot fall before the deadline for choosing it"));
            }
        }
    }
}
