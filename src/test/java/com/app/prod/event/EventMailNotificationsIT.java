package com.app.prod.event;

import com.app.prod.builders.EventPersistenceFactory;
import com.app.prod.builders.UserPersistanceFactory;
import com.app.prod.config.IntegrationTest;
import com.app.prod.config.MutableClock;
import com.app.prod.event.enums.EventMemberStatus;
import com.app.prod.event.enums.SlotPreference;
import com.app.prod.event.service.EventLifecycleService;
import com.app.prod.job.enums.JobStatus;
import com.app.prod.job.jobs.EventMailJob;
import com.app.prod.job.scheduled.ScheduledJobDrainer;
import com.app.prod.mail.MailMessage;
import com.app.prod.mail.MailSender;
import com.app.prod.mail.enums.EmailDeliveryStatus;
import com.app.prod.mail.enums.EmailDeliveryType;
import org.jooq.DSLContext;
import org.jooq.sources.tables.records.AppUserRecord;
import org.jooq.sources.tables.records.EventMemberRecord;
import org.jooq.sources.tables.records.EventRecord;
import org.jooq.sources.tables.records.EventSlotRecord;
import org.jooq.sources.tables.records.ScheduledJobRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.sources.Tables.EMAIL_DELIVERY;
import static org.jooq.sources.Tables.EVENT;
import static org.jooq.sources.Tables.EVENT_MEMBER;
import static org.jooq.sources.Tables.SCHEDULED_JOB;

/**
 * What the people of an event are told as its date gets decided. Not {@code @Transactional}: the
 * mails are booked as jobs with the change and sent by a drain, which runs every job on its own.
 */
@SpringBootTest
@Import(EventMailNotificationsIT.SynchronousMailConfig.class)
class EventMailNotificationsIT extends IntegrationTest {

    private static final Duration WALK_OUT_WINDOW = Duration.ofHours(1);

    @Autowired
    private EventPersistenceFactory events;
    @Autowired
    private UserPersistanceFactory users;
    @Autowired
    private EventLifecycleService eventLifecycleService;
    @Autowired
    private ScheduledJobDrainer scheduledJobDrainer;
    @Autowired
    private RecordingMailSender mailSender;
    @Autowired
    private DSLContext dslContext;
    @Autowired
    private MutableClock clock;

    private LocalDateTime now;
    private EventRecord event;
    private EventSlotRecord monday;
    private EventSlotRecord tuesday;

    @BeforeEach
    void setUp() {
        dslContext.deleteFrom(EVENT).execute();
        dslContext.deleteFrom(SCHEDULED_JOB).execute();
        dslContext.deleteFrom(EMAIL_DELIVERY).execute();
        mailSender.clear();
        mailSender.failFor();

        clock.setInstant(Instant.now().truncatedTo(ChronoUnit.MICROS));
        now = LocalDateTime.now(clock);
        event = events.getNewEvent().poll(2, now.plusDays(2)).name("Volleyball").buildAndSave();
        monday = slot(now.plusDays(7));
        tuesday = slot(now.plusDays(8));
    }

    @Test
    void shouldTellOnlyThoseWhoChoseTheSlotThatTheGroupFormed() {
        voter(monday, "anna@test.local");
        voter(monday, "ben@test.local");
        voter(tuesday, "clara@test.local");

        recheck(now);

        assertThat(mailSender.sent()).extracting(MailMessage::to)
                .containsExactlyInAnyOrder("anna@test.local", "ben@test.local");
        assertThat(mailSender.sent()).allSatisfy(mail -> {
            assertThat(mail.subject()).contains("Volleyball");
            assertThat(mail.body()).contains("Volleyball").doesNotContain("??");
        });
        assertThat(deliveriesOf(EmailDeliveryType.EVENT_GROUP_FORMED)).isEqualTo(2);
    }

    @Test
    void shouldTellThemAgainWhenTheGroupFormsASecondTime() {
        EventMemberRecord leaving = voter(monday, "anna@test.local");
        voter(monday, "ben@test.local");
        recheck(now);
        mailSender.clear();

        setStatus(leaving, EventMemberStatus.NOT_GOING);
        recheck(now.plusMinutes(10));
        setStatus(leaving, EventMemberStatus.GOING);
        recheck(now.plusMinutes(20));

        assertThat(mailSender.sent()).filteredOn(mail -> mail.subject().startsWith("Zebrała się grupa"))
                .extracting(MailMessage::to)
                .containsExactlyInAnyOrder("anna@test.local", "ben@test.local");
        assertThat(mailSender.sent()).filteredOn(mail -> mail.subject().startsWith("Grupa się rozpadła"))
                .extracting(MailMessage::to)
                .as("in between, the one who stayed heard it fell apart")
                .containsExactly("ben@test.local");
    }

    @Test
    void shouldWriteToAUniteAccountAtItsOwnAddress() {
        AppUserRecord account = users.getNewUser().withRandomValues().email("account@test.local").buildAndSave();
        EventMemberRecord member = events.getNewMember().withRandomValues()
                .eventId(event.getId()).status(EventMemberStatus.GOING).userId(account.getId()).buildAndSave();
        vote(monday, member);
        voter(monday, "ben@test.local");

        recheck(now);

        assertThat(mailSender.sent()).extracting(MailMessage::to)
                .containsExactlyInAnyOrder("account@test.local", "ben@test.local");
    }

    @Test
    void shouldSkipMembersWhoLeftNoAddress() {
        voter(monday, null);
        voter(monday, "ben@test.local");

        recheck(now);

        assertThat(mailSender.sent()).extracting(MailMessage::to).containsExactly("ben@test.local");
    }

    @Test
    void shouldConfirmTheDateToThoseWhoAreStillGoing() {
        voter(monday, "anna@test.local");
        voter(monday, "ben@test.local");
        EventMemberRecord leaving = voter(monday, "clara@test.local");
        recheck(now);
        setStatus(leaving, EventMemberStatus.NOT_GOING);
        mailSender.clear();

        passDeadlines(now.plus(WALK_OUT_WINDOW));

        assertThat(mailSender.sent()).extracting(MailMessage::to)
                .containsExactlyInAnyOrder("anna@test.local", "ben@test.local");
        assertThat(deliveriesOf(EmailDeliveryType.EVENT_CONFIRMED)).isEqualTo(2);
    }

    @Test
    void shouldSayOneDateIsLeftWhenTheDeadlinePassesShortOfPeople() {
        voter(tuesday, "anna@test.local");

        passDeadlines(now.plusDays(2));

        assertThat(mailSender.sent()).extracting(MailMessage::to).containsExactly("anna@test.local");
        assertThat(deliveriesOf(EmailDeliveryType.EVENT_REDUCED_TO_ONE_SLOT)).isEqualTo(1);
    }

    @Test
    void shouldTellTheGroupWhenItFallsApartWhileThereIsStillTimeToChoose() {
        EventMemberRecord leaving = voter(monday, "anna@test.local");
        voter(monday, "ben@test.local");
        voter(tuesday, "clara@test.local");
        recheck(now);
        mailSender.clear();

        setStatus(leaving, EventMemberStatus.NOT_GOING);
        recheck(now.plusMinutes(10));

        assertThat(mailSender.sent()).extracting(MailMessage::to)
                .as("those who were told the group formed and are still in it")
                .containsExactly("ben@test.local");
        assertThat(mailSender.sent().getFirst().body()).contains("stracił komplet").doesNotContain("??");
        assertThat(deliveriesOf(EmailDeliveryType.EVENT_GROUP_BROKE_UP)).isEqualTo(1);
        assertThat(deliveriesOf(EmailDeliveryType.EVENT_REDUCED_TO_ONE_SLOT)).isZero();
    }

    @Test
    void shouldSayOneDateIsLeftWhenTheGroupFallsApartAfterTheDeadline() {
        LocalDateTime deadline = event.getVotingDeadline();
        EventMemberRecord leaving = voter(monday, "anna@test.local");
        voter(monday, "ben@test.local");
        voter(tuesday, "clara@test.local");
        recheck(deadline.minusMinutes(30));
        mailSender.clear();

        setStatus(leaving, EventMemberStatus.NOT_GOING);
        recheck(deadline.plusMinutes(10));

        assertThat(mailSender.sent()).extracting(MailMessage::to)
                .as("everyone still going, the same as when the deadline itself leaves one date")
                .containsExactlyInAnyOrder("ben@test.local", "clara@test.local");
        assertThat(mailSender.sent()).allSatisfy(mail -> assertThat(mail.body()).contains("stracił komplet"));
        assertThat(deliveriesOf(EmailDeliveryType.EVENT_REDUCED_TO_ONE_SLOT)).isEqualTo(2);
        assertThat(deliveriesOf(EmailDeliveryType.EVENT_GROUP_BROKE_UP)).isZero();
    }

    @Test
    void shouldSayOneDateIsLeftEachTimeTheLastDateLosesItsGroup() {
        LocalDateTime deadline = event.getVotingDeadline();
        AppUserRecord account = users.getNewUser().withRandomValues().email("last-date@test.local").buildAndSave();
        EventMemberRecord member = events.getNewMember().withRandomValues()
                .eventId(event.getId()).status(EventMemberStatus.GOING).userId(account.getId()).buildAndSave();
        vote(monday, member);
        EventMemberRecord leaving = voter(monday, "anna@test.local");

        recheck(deadline.minusMinutes(30));
        setStatus(leaving, EventMemberStatus.NOT_GOING);
        recheck(deadline.plusMinutes(10));
        setStatus(leaving, EventMemberStatus.GOING);
        recheck(deadline.plusMinutes(20));
        setStatus(leaving, EventMemberStatus.NOT_GOING);
        recheck(deadline.plusMinutes(30));

        // a Unite account is one row per delivery and reference, so the second time needs a reference of its own
        assertThat(dslContext.fetchCount(EMAIL_DELIVERY, EMAIL_DELIVERY.DELIVERY_TYPE.eq(EmailDeliveryType.EVENT_REDUCED_TO_ONE_SLOT.name())
                .and(EMAIL_DELIVERY.USER_ID.eq(account.getId()))))
                .isEqualTo(2);
    }

    @Test
    void shouldOnlyBookTheMailUntilTheNextDrain() {
        voter(monday, "anna@test.local");
        voter(monday, "ben@test.local");

        eventLifecycleService.recheckGroup(event.getId(), now);

        assertThat(mailSender.sent()).as("nothing runs after the response on Cloud Run, so nothing is sent then").isEmpty();
        assertThat(dslContext.fetchCount(SCHEDULED_JOB, SCHEDULED_JOB.JOB_NAME.eq(EventMailJob.class.getSimpleName())
                .and(SCHEDULED_JOB.STATUS.eq(JobStatus.PENDING.name()))))
                .isEqualTo(1);

        scheduledJobDrainer.drainDue(now);

        assertThat(mailSender.sent()).extracting(MailMessage::to)
                .containsExactlyInAnyOrder("anna@test.local", "ben@test.local");
    }

    @Test
    void shouldRetryOnlyTheMailsThatDidNotGoOut() {
        voter(monday, "anna@test.local");
        voter(monday, "ben@test.local");
        mailSender.failFor("ben@test.local");

        recheck(now);

        assertThat(mailSender.sent()).extracting(MailMessage::to).containsExactly("anna@test.local");
        ScheduledJobRecord job = dslContext.selectFrom(SCHEDULED_JOB)
                .where(SCHEDULED_JOB.JOB_NAME.eq(EventMailJob.class.getSimpleName()))
                .fetchOne();
        assertThat(JobStatus.valueOf(job.getStatus())).as("back in the queue for another go").isEqualTo(JobStatus.PENDING);
        assertThat(job.getRunAt()).isAfter(now);

        mailSender.clear();
        mailSender.failFor();
        scheduledJobDrainer.drainDue(job.getRunAt());

        assertThat(mailSender.sent()).extracting(MailMessage::to)
                .as("Anna already has it")
                .containsExactly("ben@test.local");
        assertThat(dslContext.fetchCount(EMAIL_DELIVERY, EMAIL_DELIVERY.STATUS.eq(EmailDeliveryStatus.SENT.name())))
                .as("one row per person, both delivered in the end")
                .isEqualTo(2);
        assertThat(deliveriesOf(EmailDeliveryType.EVENT_GROUP_FORMED)).isEqualTo(2);
    }

    /** What a request does to the event, followed by what the next drain does with the mail it booked. */
    private void recheck(LocalDateTime at) {
        eventLifecycleService.recheckGroup(event.getId(), at);
        scheduledJobDrainer.drainDue(at);
    }

    private void passDeadlines(LocalDateTime at) {
        eventLifecycleService.applyPassedDeadlines(event.getId(), at);
        scheduledJobDrainer.drainDue(at);
    }

    private EventSlotRecord slot(LocalDateTime start) {
        return events.getNewSlot().withRandomValues().eventId(event.getId()).startDateTime(start).buildAndSave();
    }

    private EventMemberRecord voter(EventSlotRecord slot, String email) {
        EventMemberRecord member = events.getNewMember().withRandomValues()
                .eventId(event.getId()).status(EventMemberStatus.GOING).email(email).buildAndSave();
        vote(slot, member);
        return member;
    }

    private void vote(EventSlotRecord slot, EventMemberRecord member) {
        events.getNewVote().withRandomValues()
                .slotId(slot.getId()).memberId(member.getId()).preference(SlotPreference.PREFERRED).buildAndSave();
    }

    private void setStatus(EventMemberRecord member, EventMemberStatus status) {
        dslContext.update(EVENT_MEMBER)
                .set(EVENT_MEMBER.STATUS, status.name())
                .where(EVENT_MEMBER.ID.eq(member.getId()))
                .execute();
    }

    private int deliveriesOf(EmailDeliveryType type) {
        return dslContext.fetchCount(EMAIL_DELIVERY, EMAIL_DELIVERY.DELIVERY_TYPE.eq(type.name()));
    }

    @TestConfiguration
    static class SynchronousMailConfig {

        @Bean
        @Primary
        RecordingMailSender recordingMailSender() {
            return new RecordingMailSender();
        }
    }

    static class RecordingMailSender implements MailSender {

        private final Queue<MailMessage> sent = new ConcurrentLinkedQueue<>();
        private volatile Set<String> unreachable = Set.of();

        @Override
        public void send(MailMessage message) {
            if (unreachable.contains(message.to())) {
                throw new IllegalStateException("SMTP refused " + message.to());
            }
            sent.add(message);
        }

        /** These addresses fail until called again with others - no arguments makes every one work. */
        void failFor(String... addresses) {
            unreachable = Set.of(addresses);
        }

        List<MailMessage> sent() {
            return List.copyOf(sent);
        }

        void clear() {
            sent.clear();
        }
    }
}
