package com.app.prod.event;

import com.app.prod.builders.EventPersistenceFactory;
import com.app.prod.config.IntegrationTest;
import com.app.prod.config.MutableClock;
import com.app.prod.event.enums.EventMemberStatus;
import com.app.prod.event.enums.EventStatus;
import com.app.prod.event.enums.SlotPreference;
import com.app.prod.event.service.EventLifecycleService;
import com.app.prod.job.enums.JobStatus;
import com.app.prod.job.scheduled.ScheduledJobDrainer;
import org.jooq.DSLContext;
import org.jooq.sources.tables.records.EventMemberRecord;
import org.jooq.sources.tables.records.EventRecord;
import org.jooq.sources.tables.records.EventSlotRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jooq.sources.Tables.EVENT;
import static org.jooq.sources.Tables.EVENT_MEMBER;
import static org.jooq.sources.Tables.SCHEDULED_JOB;

/**
 * Not {@code @Transactional}: the drain claims its work in a transaction of its own, so a test that
 * never committed would hand it an empty queue.
 */
@SpringBootTest
class EventLifecycleIT extends IntegrationTest {

    private static final Duration WALK_OUT_WINDOW = Duration.ofHours(1);

    @Autowired
    private EventPersistenceFactory events;
    @Autowired
    private EventLifecycleService eventLifecycleService;
    @Autowired
    private ScheduledJobDrainer scheduledJobDrainer;
    @Autowired
    private DSLContext dslContext;
    @Autowired
    private MutableClock clock;

    private LocalDateTime now;
    private LocalDateTime deadline;
    private EventRecord event;
    private EventSlotRecord monday;
    private EventSlotRecord tuesday;

    @BeforeEach
    void setUp() {
        dslContext.deleteFrom(EVENT).execute();
        dslContext.deleteFrom(SCHEDULED_JOB).execute();

        // postgres stores microseconds, the clock offers nanoseconds - without this every timestamp
        // that goes through the database comes back slightly smaller than the value written
        clock.setInstant(Instant.now().truncatedTo(ChronoUnit.MICROS));
        now = LocalDateTime.now(clock);
        deadline = now.plusDays(2);
        event = events.getNewEvent().poll(2, deadline).buildAndSave();
        monday = slot(now.plusDays(7));
        tuesday = slot(now.plusDays(8));
    }

    @Test
    void shouldFormTheGroupOnceASlotHasEnoughPeople() {
        voter(monday, SlotPreference.PREFERRED);
        voter(monday, SlotPreference.PREFERRED);

        eventLifecycleService.recheckGroup(event.getId(), now);

        EventRecord formed = reload();
        assertThat(EventStatus.valueOf(formed.getStatus())).isEqualTo(EventStatus.GROUP_FORMED);
        assertThat(formed.getSelectedSlotId()).isEqualTo(monday.getId());
        assertThat(formed.getConfirmBy()).isEqualTo(now.plus(WALK_OUT_WINDOW));
    }

    @Test
    void shouldLeaveTheEventCollectingWhileEverySlotIsShort() {
        voter(monday, SlotPreference.PREFERRED);
        voter(tuesday, SlotPreference.IF_NEEDED);

        eventLifecycleService.recheckGroup(event.getId(), now);

        assertThat(EventStatus.valueOf(reload().getStatus())).isEqualTo(EventStatus.COLLECTING_VOTES);
        assertThat(reload().getSelectedSlotId()).isNull();
    }

    @Test
    void shouldBookTheEndOfTheWalkOutWindowAsAJob() {
        voter(monday, SlotPreference.PREFERRED);
        voter(monday, SlotPreference.PREFERRED);

        eventLifecycleService.recheckGroup(event.getId(), now);

        assertThat(jobRunAt("event_grace_window:" + event.getId())).isEqualTo(now.plus(WALK_OUT_WINDOW));
    }

    @Test
    void shouldGoBackToCollectingWhenSomeoneWalksOutOfTheWindow() {
        EventMemberRecord leaving = voter(monday, SlotPreference.PREFERRED);
        voter(monday, SlotPreference.PREFERRED);
        eventLifecycleService.recheckGroup(event.getId(), now);

        walkOut(leaving);
        eventLifecycleService.recheckGroup(event.getId(), now.plusMinutes(10));

        EventRecord released = reload();
        assertThat(EventStatus.valueOf(released.getStatus()))
                .as("having enough people is the condition for the group existing, not a gate passed once")
                .isEqualTo(EventStatus.COLLECTING_VOTES);
        assertThat(released.getSelectedSlotId()).isNull();
        assertThat(released.getConfirmBy()).isNull();
        assertThat(jobStatus("event_grace_window:" + event.getId())).isEqualTo(JobStatus.CANCELLED);
    }

    @Test
    void shouldKeepTheGroupWaitingWhileTheWindowIsStillRunning() {
        voter(monday, SlotPreference.PREFERRED);
        voter(monday, SlotPreference.PREFERRED);
        eventLifecycleService.recheckGroup(event.getId(), now);

        eventLifecycleService.applyPassedDeadlines(event.getId(), now.plus(WALK_OUT_WINDOW).minusMinutes(1));

        assertThat(EventStatus.valueOf(reload().getStatus())).isEqualTo(EventStatus.GROUP_FORMED);
    }

    @Test
    void shouldConfirmTheEventOnceTheWindowElapses() {
        voter(monday, SlotPreference.PREFERRED);
        voter(monday, SlotPreference.PREFERRED);
        eventLifecycleService.recheckGroup(event.getId(), now);

        eventLifecycleService.applyPassedDeadlines(event.getId(), now.plus(WALK_OUT_WINDOW));

        EventRecord confirmed = reload();
        assertThat(EventStatus.valueOf(confirmed.getStatus())).isEqualTo(EventStatus.CONFIRMED);
        assertThat(confirmed.getStartDateTime())
                .as("a confirmed event becomes an ordinary dated event")
                .isEqualTo(monday.getStartDateTime());
        assertThat(confirmed.getConfirmBy()).isNull();
    }

    @Test
    void shouldSeatTheVotersOfTheWinningSlotAndExcuseTheRest() {
        EventMemberRecord anna = voter(monday, SlotPreference.PREFERRED);
        EventMemberRecord bob = voter(monday, SlotPreference.PREFERRED);
        EventMemberRecord clara = voter(tuesday, SlotPreference.PREFERRED);

        eventLifecycleService.recheckGroup(event.getId(), now);
        eventLifecycleService.applyPassedDeadlines(event.getId(), now.plus(WALK_OUT_WINDOW));

        assertThat(statusOf(anna)).isEqualTo(EventMemberStatus.GOING);
        assertThat(statusOf(bob)).isEqualTo(EventMemberStatus.GOING);
        assertThat(statusOf(clara))
                .as("clara never said no - the date simply went the other way")
                .isEqualTo(EventMemberStatus.NOT_AVAILABLE);
    }

    @Test
    void shouldWaitlistWhoeverDoesNotFit() {
        event = events.getNewEvent().poll(2, deadline).maxAttendees(2).waitlistEnabled(true).buildAndSave();
        monday = slot(now.plusDays(7));

        EventMemberRecord first = voter(monday, SlotPreference.PREFERRED);
        EventMemberRecord second = voter(monday, SlotPreference.PREFERRED);
        EventMemberRecord third = voter(monday, SlotPreference.IF_NEEDED);

        eventLifecycleService.recheckGroup(event.getId(), now);
        eventLifecycleService.applyPassedDeadlines(event.getId(), now.plus(WALK_OUT_WINDOW));

        assertThat(statusOf(first)).isEqualTo(EventMemberStatus.GOING);
        assertThat(statusOf(second)).isEqualTo(EventMemberStatus.GOING);
        assertThat(statusOf(third))
                .as("a reluctant yes gives up its seat to a plain one")
                .isEqualTo(EventMemberStatus.WAITLIST);
    }

    @Test
    void shouldKeepOnlyTheBestSlotWhenTheDeadlinePassesShortOfPeople() {
        voter(tuesday, SlotPreference.PREFERRED);

        eventLifecycleService.applyPassedDeadlines(event.getId(), deadline);

        EventRecord reduced = reload();
        assertThat(EventStatus.valueOf(reduced.getStatus())).isEqualTo(EventStatus.ONE_SLOT_LEFT);
        assertThat(reduced.getSelectedSlotId()).isEqualTo(tuesday.getId());
    }

    @Test
    void shouldLetOnlyTheSurvivingSlotCarryTheEventAfterTheDeadline() {
        voter(tuesday, SlotPreference.PREFERRED);
        eventLifecycleService.applyPassedDeadlines(event.getId(), deadline);

        voter(monday, SlotPreference.PREFERRED);
        voter(monday, SlotPreference.PREFERRED);
        eventLifecycleService.recheckGroup(event.getId(), deadline.plusHours(1));

        assertThat(EventStatus.valueOf(reload().getStatus()))
                .as("the dropped slots are gone for good, however many people would now take them")
                .isEqualTo(EventStatus.ONE_SLOT_LEFT);

        voter(tuesday, SlotPreference.PREFERRED);
        eventLifecycleService.recheckGroup(event.getId(), deadline.plusHours(2));

        assertThat(EventStatus.valueOf(reload().getStatus())).isEqualTo(EventStatus.GROUP_FORMED);
    }

    @Test
    void shouldLetTheHostConfirmWithoutWaitingOutTheWindow() {
        voter(monday, SlotPreference.PREFERRED);
        voter(monday, SlotPreference.PREFERRED);
        eventLifecycleService.recheckGroup(event.getId(), now);

        eventLifecycleService.startEventNow(event.getId(), now.plusMinutes(1));

        assertThat(EventStatus.valueOf(reload().getStatus())).isEqualTo(EventStatus.CONFIRMED);
        assertThat(jobStatus("event_grace_window:" + event.getId())).isEqualTo(JobStatus.CANCELLED);
    }

    @Test
    void shouldLeaveADatedEventAlone() {
        EventRecord fixed = events.getNewEvent().fixed().buildAndSave();

        eventLifecycleService.recheckGroup(fixed.getId(), now);
        eventLifecycleService.applyPassedDeadlines(fixed.getId(), now.plusYears(1));

        assertThat(EventStatus.valueOf(reload(fixed.getId()).getStatus())).isEqualTo(EventStatus.CONFIRMED);
    }

    @Test
    void shouldShrugOffAJobForAnEventThatIsGone() {
        assertThatCode(() -> eventLifecycleService.applyPassedDeadlines(UUID.randomUUID(), now))
                .as("a booked job outlives the event it was booked for")
                .doesNotThrowAnyException();
    }

    @Test
    void shouldMeetTheDeadlineThroughTheJobQueue() {
        voter(tuesday, SlotPreference.PREFERRED);
        eventLifecycleService.scheduleDeadlineCheck(event.getId(), deadline);

        clock.setInstant(deadline.atZone(clock.getZone()).toInstant());
        scheduledJobDrainer.drainDue(deadline);

        assertThat(EventStatus.valueOf(reload().getStatus()))
                .as("the deadline has to fire with nobody watching")
                .isEqualTo(EventStatus.ONE_SLOT_LEFT);
        assertThat(jobStatus("event_voting_deadline:" + event.getId())).isEqualTo(JobStatus.SUCCESS);
    }

    private EventSlotRecord slot(LocalDateTime start) {
        return events.getNewSlot().withRandomValues().eventId(event.getId()).startDateTime(start).buildAndSave();
    }

    private EventMemberRecord voter(EventSlotRecord slot, SlotPreference preference) {
        EventMemberRecord member = events.getNewMember().withRandomValues()
                .eventId(event.getId()).status(EventMemberStatus.GOING).buildAndSave();
        events.getNewVote().withRandomValues()
                .slotId(slot.getId()).memberId(member.getId()).preference(preference).buildAndSave();
        return member;
    }

    private void walkOut(EventMemberRecord member) {
        dslContext.update(EVENT_MEMBER)
                .set(EVENT_MEMBER.STATUS, EventMemberStatus.NOT_GOING.name())
                .where(EVENT_MEMBER.ID.eq(member.getId()))
                .execute();
    }

    private EventMemberStatus statusOf(EventMemberRecord member) {
        return EventMemberStatus.valueOf(dslContext.select(EVENT_MEMBER.STATUS)
                .from(EVENT_MEMBER)
                .where(EVENT_MEMBER.ID.eq(member.getId()))
                .fetchOne(EVENT_MEMBER.STATUS));
    }

    private EventRecord reload() {
        return reload(event.getId());
    }

    private EventRecord reload(UUID eventId) {
        return dslContext.selectFrom(EVENT).where(EVENT.ID.eq(eventId)).fetchOne();
    }

    private LocalDateTime jobRunAt(String dedupeKey) {
        return dslContext.select(SCHEDULED_JOB.RUN_AT).from(SCHEDULED_JOB)
                .where(SCHEDULED_JOB.DEDUPE_KEY.eq(dedupeKey))
                .fetchOne(SCHEDULED_JOB.RUN_AT);
    }

    private JobStatus jobStatus(String dedupeKey) {
        return JobStatus.valueOf(dslContext.select(SCHEDULED_JOB.STATUS).from(SCHEDULED_JOB)
                .where(SCHEDULED_JOB.DEDUPE_KEY.eq(dedupeKey))
                .orderBy(SCHEDULED_JOB.CREATED_AT.desc())
                .limit(1)
                .fetchOne(SCHEDULED_JOB.STATUS));
    }
}
