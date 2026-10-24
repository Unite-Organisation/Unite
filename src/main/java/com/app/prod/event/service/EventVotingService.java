package com.app.prod.event.service;

import com.app.prod.event.dto.SlotVote;
import com.app.prod.event.dto.SlotVoteRequest;
import com.app.prod.event.enums.EventMemberStatus;
import com.app.prod.event.enums.EventStatus;
import com.app.prod.event.enums.SchedulingMode;
import com.app.prod.event.repository.EventMemberRepository;
import com.app.prod.event.repository.EventRepository;
import com.app.prod.event.repository.EventSlotRepository;
import com.app.prod.event.repository.EventSlotVoteRepository;
import com.app.prod.event.web.EventScope;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.EventMemberRecord;
import org.jooq.sources.tables.records.EventRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventVotingService {

    private final EventRepository eventRepository;
    private final EventMemberRepository eventMemberRepository;
    private final EventSlotRepository eventSlotRepository;
    private final EventSlotVoteRepository eventSlotVoteRepository;
    private final EventLifecycleService eventLifecycleService;
    private final Clock clock;

    @Transactional
    public void replaceVotes(EventScope scope, List<SlotVoteRequest> votes) {
        EventRecord event = eventRepository.findForUpdate(scope.eventId())
                .orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.EVENT_NOT_FOUND)));
        LocalDateTime now = LocalDateTime.now(clock);

        save(event, scope.memberId(), votes, now);
        eventLifecycleService.recheckGroup(event.getId(), now);
    }

    void save(EventRecord event, UUID memberId, List<SlotVoteRequest> votes, LocalDateTime now) {
        requireChoosingDate(event);
        requireVotingOpen(event);

        if (votes == null || votes.isEmpty()) {
            throw new BadRequestException(AppError.of(Code.EVENT_VOTES_REQUIRED));
        }

        List<UUID> slotIds = votes.stream().map(SlotVoteRequest::slotId).toList();
        if (!eventSlotRepository.allBelongToEvent(event.getId(), slotIds)) {
            throw new EntityNotPresentException(AppError.of(Code.EVENT_SLOT_NOT_FOUND));
        }
        requireSlotsStillOpen(event, slotIds);

        eventSlotVoteRepository.replaceForMember(
                memberId,
                votes.stream()
                    .map(vote -> new SlotVote(vote.slotId(), vote.preference()))
                    .toList(),
                now
        );

        markAsJoined(memberId, now);
        log.info("Member {} chose {} date(s) of event {}", memberId, votes.size(), event.getId());
    }

    private void markAsJoined(UUID memberId, LocalDateTime now) {
        EventMemberRecord member = eventMemberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.EVENT_NOT_FOUND)));

        if (EventMemberStatus.valueOf(member.getStatus()) == EventMemberStatus.UNDECIDED) {
            member.setStatus(EventMemberStatus.GOING.name());
            member.setStatusChangedAt(now);
            eventMemberRepository.update(member);
        }
    }

    private static void requireChoosingDate(EventRecord event) {
        if (SchedulingMode.valueOf(event.getSchedulingMode()) != SchedulingMode.POLL) {
            throw new BadRequestException(AppError.of(Code.EVENT_MODE_MISMATCH, "This event already has a date"));
        }
    }

    private static void requireVotingOpen(EventRecord event) {
        EventStatus status = EventStatus.valueOf(event.getStatus());
        if (status != EventStatus.COLLECTING_VOTES && status != EventStatus.ONE_SLOT_LEFT) {
            throw new BadRequestException(AppError.of(Code.EVENT_VOTING_CLOSED));
        }
    }

    private static void requireSlotsStillOpen(EventRecord event, List<UUID> slotIds) {
        if (event.getSelectedSlotId() == null) {
            return;
        }
        if (!slotIds.stream().allMatch(slotId -> slotId.equals(event.getSelectedSlotId()))) {
            throw new BadRequestException(AppError.of(Code.EVENT_SLOT_NOT_AVAILABLE));
        }
    }
}
