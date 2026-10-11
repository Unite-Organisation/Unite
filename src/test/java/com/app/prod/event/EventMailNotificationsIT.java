package com.app.prod.event;

import com.app.prod.builders.EventPersistenceFactory;
import com.app.prod.builders.UserPersistanceFactory;
import com.app.prod.config.IntegrationTest;
import com.app.prod.config.MutableClock;
import com.app.prod.event.enums.EventMemberStatus;
import com.app.prod.event.enums.SlotPreference;
import com.app.prod.event.service.EventLifecycleService;
import com.app.prod.eventbus.EventExecutor;
import com.app.prod.mail.MailMessage;
import com.app.prod.mail.MailSender;
import com.app.prod.mail.enums.EmailDeliveryType;
import org.jooq.DSLContext;
import org.jooq.sources.tables.records.AppUserRecord;
import org.jooq.sources.tables.records.EventMemberRecord;
import org.jooq.sources.tables.records.EventRecord;
import org.jooq.sources.tables.records.EventSlotRecord;
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
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.sources.Tables.EMAIL_DELIVERY;
import static org.jooq.sources.Tables.EVENT;
import static org.jooq.sources.Tables.EVENT_MEMBER;
import static org.jooq.sources.Tables.SCHEDULED_JOB;

/**
 * What the people of an event are told as its date gets decided. Not {@code @Transactional}: the
 * mails go out after the commit, which a test wrapped in a transaction would never reach.
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

        eventLifecycleService.recheckGroup(event.getId(), now);

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
        eventLifecycleService.recheckGroup(event.getId(), now);
        mailSender.clear();

        setStatus(leaving, EventMemberStatus.NOT_GOING);
        eventLifecycleService.recheckGroup(event.getId(), now.plusMinutes(10));
        setStatus(leaving, EventMemberStatus.GOING);
        eventLifecycleService.recheckGroup(event.getId(), now.plusMinutes(20));

        assertThat(mailSender.sent()).extracting(MailMessage::to)
                .containsExactlyInAnyOrder("anna@test.local", "ben@test.local");
    }

    @Test
    void shouldWriteToAUniteAccountAtItsOwnAddress() {
        AppUserRecord account = users.getNewUser().withRandomValues().email("account@test.local").buildAndSave();
        EventMemberRecord member = events.getNewMember().withRandomValues()
                .eventId(event.getId()).status(EventMemberStatus.GOING).userId(account.getId()).buildAndSave();
        vote(monday, member);
        voter(monday, "ben@test.local");

        eventLifecycleService.recheckGroup(event.getId(), now);

        assertThat(mailSender.sent()).extracting(MailMessage::to)
                .containsExactlyInAnyOrder("account@test.local", "ben@test.local");
    }

    @Test
    void shouldSkipMembersWhoLeftNoAddress() {
        voter(monday, null);
        voter(monday, "ben@test.local");

        eventLifecycleService.recheckGroup(event.getId(), now);

        assertThat(mailSender.sent()).extracting(MailMessage::to).containsExactly("ben@test.local");
    }

    @Test
    void shouldConfirmTheDateToThoseWhoAreStillGoing() {
        voter(monday, "anna@test.local");
        voter(monday, "ben@test.local");
        EventMemberRecord leaving = voter(monday, "clara@test.local");
        eventLifecycleService.recheckGroup(event.getId(), now);
        setStatus(leaving, EventMemberStatus.NOT_GOING);
        mailSender.clear();

        eventLifecycleService.applyPassedDeadlines(event.getId(), now.plus(WALK_OUT_WINDOW));

        assertThat(mailSender.sent()).extracting(MailMessage::to)
                .containsExactlyInAnyOrder("anna@test.local", "ben@test.local");
        assertThat(deliveriesOf(EmailDeliveryType.EVENT_CONFIRMED)).isEqualTo(2);
    }

    @Test
    void shouldSayOneDateIsLeftWhenTheDeadlinePassesShortOfPeople() {
        voter(tuesday, "anna@test.local");

        eventLifecycleService.applyPassedDeadlines(event.getId(), now.plusDays(2));

        assertThat(mailSender.sent()).extracting(MailMessage::to).containsExactly("anna@test.local");
        assertThat(deliveriesOf(EmailDeliveryType.EVENT_REDUCED_TO_ONE_SLOT)).isEqualTo(1);
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
        EventExecutor sameThreadEventExecutor() {
            return Runnable::run;
        }

        @Bean
        @Primary
        RecordingMailSender recordingMailSender() {
            return new RecordingMailSender();
        }
    }

    static class RecordingMailSender implements MailSender {

        private final Queue<MailMessage> sent = new ConcurrentLinkedQueue<>();

        @Override
        public void send(MailMessage message) {
            sent.add(message);
        }

        List<MailMessage> sent() {
            return List.copyOf(sent);
        }

        void clear() {
            sent.clear();
        }
    }
}
