package com.app.prod.event.service;

import com.app.prod.event.dto.RankedSlot;
import com.app.prod.event.dto.SlotTally;
import com.app.prod.event.enums.EventMemberStatus;
import com.app.prod.event.enums.EventStatus;
import com.app.prod.event.enums.SchedulingMode;
import com.app.prod.event.events.EventConfirmedEvent;
import com.app.prod.event.events.EventGroupFormedEvent;
import com.app.prod.event.events.EventReducedToOneSlotEvent;
import com.app.prod.event.repository.EventMemberRepository;
import com.app.prod.event.repository.EventRepository;
import com.app.prod.event.repository.EventSlotRepository;
import com.app.prod.eventbus.EventBus;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.exceptions.exceptions.IllegalApplicationStateException;
import com.app.prod.job.jobs.EventDeadlineJob;
import com.app.prod.job.scheduled.ScheduledJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.EventRecord;
import org.jooq.sources.tables.records.EventSlotRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventLifecycleService {
    static final Duration WALK_OUT_WINDOW = Duration.ofHours(1);

    private final EventRepository eventRepository;
    private final EventMemberRepository eventMemberRepository;
    private final EventSlotRepository eventSlotRepository;
    private final ScheduledJobService scheduledJobService;
    private final EventBus eventBus;

    public void scheduleDeadlineCheck(UUID eventId, LocalDateTime votingDeadline) {
        scheduledJobService.schedule(new EventDeadlineJob(eventId), votingDeadline, votingDeadlineJobKey(eventId));
    }

    @Transactional
    public void recheckGroup(UUID eventId, LocalDateTime now) {
        EventRecord event = eventRepository.findForUpdate(eventId)
                .orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.EVENT_NOT_FOUND)));
        if (hasFixedDate(event)) {
            return;
        }

        switch (EventStatus.valueOf(event.getStatus())) {
            case COLLECTING_VOTES, ONE_SLOT_LEFT -> formGroupIfEnoughPeople(event, now);
            case GROUP_FORMED -> breakUpGroupIfTooFewPeople(event, now);
            case CONFIRMED -> { }
        }
    }

    @Transactional
    public void applyPassedDeadlines(UUID eventId, LocalDateTime now) {
        // the job outlives whatever it was booked for; an event that is gone is not an error
        Optional<EventRecord> found = eventRepository.findForUpdate(eventId);
        if (found.isEmpty() || hasFixedDate(found.get())) {
            return;
        }
        EventRecord event = found.get();

        switch (EventStatus.valueOf(event.getStatus())) {
            case COLLECTING_VOTES -> {
                if (!event.getVotingDeadline().isAfter(now)) {
                    keepOnlyBestSlot(event, now);
                }
            }
            case GROUP_FORMED -> {
                if (event.getConfirmBy() != null && !event.getConfirmBy().isAfter(now)) {
                    confirmEvent(event, now);
                }
            }
            default -> { }
        }
    }

    /** The host cutting the walk-out window short once the group is together. */
    @Transactional
    public void startEventNow(UUID eventId, LocalDateTime now) {
        EventRecord event = eventRepository.findForUpdate(eventId)
                .orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.EVENT_NOT_FOUND)));
        if (hasFixedDate(event)) {
            throw new BadRequestException(AppError.of(Code.EVENT_MODE_MISMATCH, "A dated event is already confirmed"));
        }
        if (EventStatus.valueOf(event.getStatus()) == EventStatus.CONFIRMED) {
            return;
        }
        if (event.getSelectedSlotId() == null) {
            throw new BadRequestException(AppError.of(Code.EVENT_SLOT_NOT_CHOSEN));
        }

        confirmEvent(event, now);
        scheduledJobService.cancel(walkOutWindowJobKey(eventId));
    }

    private void formGroupIfEnoughPeople(EventRecord event, LocalDateTime now) {
        SlotRanking.qualifying(slotsStillOpen(event), event.getMinAttendees())
                .ifPresent(slot -> formGroup(event, slot.slotId(), now));
    }

    private void formGroup(EventRecord event, UUID slotId, LocalDateTime now) {
        LocalDateTime confirmBy = now.plus(WALK_OUT_WINDOW);

        event.setStatus(EventStatus.GROUP_FORMED.name());
        event.setSelectedSlotId(slotId);
        event.setConfirmBy(confirmBy);
        eventRepository.update(event);

        scheduledJobService.schedule(new EventDeadlineJob(event.getId()), confirmBy, walkOutWindowJobKey(event.getId()));
        eventBus.publish(new EventGroupFormedEvent(event.getId(), slotId, confirmBy));
        log.info("Event {} reached its threshold on slot {}, members may walk out until {}", event.getId(), slotId, confirmBy);
    }

    private void breakUpGroupIfTooFewPeople(EventRecord event, LocalDateTime now) {
        int carrying = slotsStillOpen(event).stream().mapToInt(SlotTally::total).sum();
        if (carrying >= event.getMinAttendees()) {
            return;
        }

        boolean stillVoting = event.getVotingDeadline().isAfter(now);
        event.setStatus(stillVoting ? EventStatus.COLLECTING_VOTES.name() : EventStatus.ONE_SLOT_LEFT.name());
        event.setConfirmBy(null);
        if (stillVoting) {
            event.setSelectedSlotId(null);
        }
        eventRepository.update(event);

        scheduledJobService.cancel(walkOutWindowJobKey(event.getId()));
        log.info("Event {} dropped to {} of {} and went back to collecting", event.getId(), carrying, event.getMinAttendees());
    }

    private void keepOnlyBestSlot(EventRecord event, LocalDateTime now) {
        List<SlotTally> tallies = eventSlotRepository.tallies(event.getId());
        Optional<RankedSlot> best = SlotRanking.best(tallies, event.getMinAttendees());

        if (best.isEmpty()) {
            log.warn("Event {} reached its deadline with no slots to fall back on", event.getId());
            return;
        }
        if (best.get().qualifies()) {
            formGroup(event, best.get().slotId(), now);
            return;
        }

        event.setStatus(EventStatus.ONE_SLOT_LEFT.name());
        event.setSelectedSlotId(best.get().slotId());
        eventRepository.update(event);

        eventBus.publish(new EventReducedToOneSlotEvent(event.getId(), best.get().slotId()));
        log.info("Event {} missed its threshold and reduced to one slot: {}", event.getId(), best.get().slotId());
    }

    private void confirmEvent(EventRecord event, LocalDateTime now) {
        UUID slotId = event.getSelectedSlotId();
        EventSlotRecord slot = eventSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalApplicationStateException(AppError.of(Code.EVENT_SLOT_NOT_FOUND, String.format("Event %s points at slot %s", event.getId(), slotId))));

        giveSeatsToVoters(event, slotId, now);

        event.setStatus(EventStatus.CONFIRMED.name());
        event.setStartDateTime(slot.getStartDateTime());
        event.setEndDateTime(slot.getEndDateTime());
        event.setConfirmBy(null);
        eventRepository.update(event);

        eventBus.publish(new EventConfirmedEvent(event.getId(), slotId, slot.getStartDateTime()));
        log.info("Event {} confirmed on slot {} starting {}", event.getId(), slotId, slot.getStartDateTime());
    }

    private void giveSeatsToVoters(EventRecord event, UUID slotId, LocalDateTime now) {
        List<UUID> claimants = eventMemberRepository.findGoingVotersForSlot(event.getId(), slotId);
        int seats = event.getMaxAttendees() == null
                ? claimants.size()
                : Math.min(event.getMaxAttendees(), claimants.size());

        eventMemberRepository.moveStatus(event.getId(), EventMemberStatus.GOING, EventMemberStatus.NOT_AVAILABLE, now);
        eventMemberRepository.setStatus(claimants.subList(0, seats), EventMemberStatus.GOING, now);

        List<UUID> overflow = claimants.subList(seats, claimants.size());
        if (!overflow.isEmpty()) {
            EventMemberStatus status = Boolean.TRUE.equals(event.getWaitlistEnabled())
                    ? EventMemberStatus.WAITLIST
                    : EventMemberStatus.NOT_AVAILABLE;
            eventMemberRepository.setStatus(overflow, status, now);
            log.info("Event {} seated {} members, {} over the limit", event.getId(), seats, overflow.size());
        }
    }

    private List<SlotTally> slotsStillOpen(EventRecord event) {
        List<SlotTally> tallies = eventSlotRepository.tallies(event.getId());
        if (event.getSelectedSlotId() == null) {
            return tallies;
        }
        return tallies.stream()
                .filter(tally -> tally.slotId().equals(event.getSelectedSlotId()))
                .toList();
    }

    private static boolean hasFixedDate(EventRecord event) {
        return SchedulingMode.valueOf(event.getSchedulingMode()) == SchedulingMode.FIXED;
    }

    private static String votingDeadlineJobKey(UUID eventId) {
        return "event_voting_deadline:" + eventId;
    }

    private static String walkOutWindowJobKey(UUID eventId) {
        return "event_grace_window:" + eventId;
    }
}
