package com.app.prod.event;

import com.app.prod.builders.EventPersistenceFactory;
import com.app.prod.config.IntegrationTest;
import com.app.prod.config.MutableClock;
import com.app.prod.event.dto.SlotTally;
import com.app.prod.event.dto.SlotVote;
import com.app.prod.event.enums.EventMemberStatus;
import com.app.prod.event.enums.SlotPreference;
import com.app.prod.event.repository.EventSlotRepository;
import com.app.prod.event.repository.EventSlotVoteRepository;
import org.jooq.sources.tables.records.EventMemberRecord;
import org.jooq.sources.tables.records.EventRecord;
import org.jooq.sources.tables.records.EventSlotRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class EventSlotRepositoryIT extends IntegrationTest {

    @Autowired
    private EventPersistenceFactory events;
    @Autowired
    private EventSlotRepository eventSlotRepository;
    @Autowired
    private EventSlotVoteRepository eventSlotVoteRepository;
    @Autowired
    private MutableClock clock;

    private EventRecord event;
    private EventSlotRecord monday;
    private EventSlotRecord tuesday;
    private EventSlotRecord wednesday;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now(clock);
        event = events.getNewEvent().poll(3, now.plusDays(2)).buildAndSave();

        monday = slot(now.plusDays(7));
        tuesday = slot(now.plusDays(8));
        wednesday = slot(now.plusDays(9));
    }

    @Test
    void shouldCountEachPreferenceSeparately() {
        EventMemberRecord anna = member(EventMemberStatus.GOING);
        EventMemberRecord bob = member(EventMemberStatus.GOING);

        vote(monday, anna, SlotPreference.PREFERRED);
        vote(monday, bob, SlotPreference.PREFERRED);
        vote(tuesday, anna, SlotPreference.IF_NEEDED);

        Map<UUID, SlotTally> tallies = talliesById();

        assertThat(tallies.get(monday.getId()).preferred()).isEqualTo(2);
        assertThat(tallies.get(monday.getId()).ifNeeded()).isZero();
        assertThat(tallies.get(tuesday.getId()).preferred()).isZero();
        assertThat(tallies.get(tuesday.getId()).ifNeeded()).isEqualTo(1);
    }

    @Test
    void shouldStopCountingAMemberWhoWalkedOut() {
        EventMemberRecord staying = member(EventMemberStatus.GOING);
        EventMemberRecord left = member(EventMemberStatus.NOT_GOING);

        vote(monday, staying, SlotPreference.PREFERRED);
        vote(monday, left, SlotPreference.PREFERRED);

        assertThat(talliesById().get(monday.getId()).preferred())
                .as("a vote outlives the member leaving, but must stop counting towards the threshold")
                .isEqualTo(1);
    }

    @Test
    void shouldReturnSlotsNobodyVotedFor() {
        vote(monday, member(EventMemberStatus.GOING), SlotPreference.PREFERRED);

        Map<UUID, SlotTally> tallies = talliesById();

        assertThat(tallies).hasSize(3);
        assertThat(tallies.get(wednesday.getId()).total()).isZero();
    }

    @Test
    void shouldReturnSlotsOfThisEventOnly() {
        EventRecord other = events.getNewEvent().poll(3, now.plusDays(2)).buildAndSave();
        events.getNewSlot().withRandomValues().eventId(other.getId()).startDateTime(now.plusDays(7)).buildAndSave();

        assertThat(eventSlotRepository.tallies(event.getId())).hasSize(3);
    }

    @Test
    void shouldReturnTalliesOldestSlotFirst() {
        List<SlotTally> tallies = eventSlotRepository.tallies(event.getId());

        assertThat(tallies).extracting(SlotTally::slotId)
                .containsExactly(monday.getId(), tuesday.getId(), wednesday.getId());
    }

    @Test
    void shouldAcceptSlotsThatBelongToTheEvent() {
        assertThat(eventSlotRepository.allBelongToEvent(event.getId(), List.of(monday.getId(), tuesday.getId()))).isTrue();
    }

    @Test
    void shouldNotBeFooledByTheSameSlotNamedTwice() {
        assertThat(eventSlotRepository.allBelongToEvent(event.getId(), List.of(monday.getId(), monday.getId()))).isTrue();
    }

    @Test
    void shouldRejectASlotOfAnotherEvent() {
        EventRecord other = events.getNewEvent().poll(3, now.plusDays(2)).buildAndSave();
        EventSlotRecord foreign = events.getNewSlot().withRandomValues()
                .eventId(other.getId()).startDateTime(now.plusDays(7)).buildAndSave();

        assertThat(eventSlotRepository.allBelongToEvent(event.getId(), List.of(monday.getId(), foreign.getId())))
                .as("voting must not be able to reach into another event's slots")
                .isFalse();
    }

    @Test
    void shouldWriteAMembersChoiceWholeRatherThanAddingToIt() {
        EventMemberRecord anna = member(EventMemberStatus.GOING);
        vote(monday, anna, SlotPreference.PREFERRED);
        vote(tuesday, anna, SlotPreference.PREFERRED);

        eventSlotVoteRepository.replaceForMember(anna.getId(), List.of(
                new SlotVote(tuesday.getId(), SlotPreference.IF_NEEDED),
                new SlotVote(wednesday.getId(), SlotPreference.PREFERRED)
        ), now);

        assertThat(eventSlotVoteRepository.findByMember(anna.getId()))
                .as("monday was dropped, tuesday changed its mind, wednesday is new")
                .containsExactlyInAnyOrder(
                        new SlotVote(tuesday.getId(), SlotPreference.IF_NEEDED),
                        new SlotVote(wednesday.getId(), SlotPreference.PREFERRED)
                );
    }

    @Test
    void shouldClearAMembersChoiceWhenNothingIsLeftOfIt() {
        EventMemberRecord anna = member(EventMemberStatus.GOING);
        vote(monday, anna, SlotPreference.PREFERRED);

        eventSlotVoteRepository.replaceForMember(anna.getId(), List.of(), now);

        assertThat(eventSlotVoteRepository.findByMember(anna.getId())).isEmpty();
    }

    private Map<UUID, SlotTally> talliesById() {
        return eventSlotRepository.tallies(event.getId()).stream()
                .collect(java.util.stream.Collectors.toMap(SlotTally::slotId, Function.identity()));
    }

    private EventSlotRecord slot(LocalDateTime start) {
        return events.getNewSlot().withRandomValues().eventId(event.getId()).startDateTime(start).buildAndSave();
    }

    private EventMemberRecord member(EventMemberStatus status) {
        return events.getNewMember().withRandomValues().eventId(event.getId()).status(status).buildAndSave();
    }

    private void vote(EventSlotRecord slot, EventMemberRecord member, SlotPreference preference) {
        events.getNewVote().withRandomValues()
                .slotId(slot.getId()).memberId(member.getId()).preference(preference).buildAndSave();
    }
}
