package com.app.prod.event;

import com.app.prod.builders.UserPersistanceFactory;
import com.app.prod.config.IntegrationTest;
import com.app.prod.config.MutableClock;
import com.app.prod.event.EventApi.Event;
import com.app.prod.event.EventApi.Person;
import com.app.prod.event.EventMailNotificationsIT.RecordingMailSender;
import com.app.prod.event.dto.CreateEventRequest;
import com.app.prod.event.dto.OpenSessionRequest;
import com.app.prod.job.scheduled.ScheduledJobDrainer;
import com.app.prod.mail.MailMessage;
import com.app.prod.mail.enums.EmailDeliveryStatus;
import com.app.prod.mail.enums.EmailDeliveryType;
import com.app.prod.user.enums.UserRole;
import com.app.prod.utils.JwtTestHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.jooq.sources.tables.records.AppUserRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.sources.Tables.EMAIL_DELIVERY;
import static org.jooq.sources.Tables.EVENT;
import static org.jooq.sources.Tables.EVENT_MEMBER;
import static org.jooq.sources.Tables.SCHEDULED_JOB;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A guest joining an event is mailed the code that brings them back. Not {@code @Transactional}: the mail is a job, sent by a drain. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(EventMailNotificationsIT.SynchronousMailConfig.class)
class EventReturnCodeMailIT extends IntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private DSLContext dslContext;
    @Autowired
    private MutableClock clock;
    @Autowired
    private UserPersistanceFactory users;
    @Autowired
    private JwtTestHelper jwt;
    @Autowired
    private RecordingMailSender mailSender;
    @Autowired
    private ScheduledJobDrainer scheduledJobDrainer;

    private EventApi api;
    private Event event;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        dslContext.deleteFrom(EVENT).execute();
        dslContext.deleteFrom(SCHEDULED_JOB).execute();
        dslContext.deleteFrom(EMAIL_DELIVERY).execute();
        mailSender.clear();
        mailSender.failFor();

        clock.setInstant(Instant.now().truncatedTo(ChronoUnit.MICROS));
        now = LocalDateTime.now(clock);
        api = new EventApi(mvc, objectMapper);
        event = api.createDatedEvent("Host", now.plusDays(7), null, false);

        // the host's own code is a mail of its own, out of the way of what each test counts
        scheduledJobDrainer.drainDue(now);
        mailSender.clear();
        dslContext.deleteFrom(EMAIL_DELIVERY).execute();
    }

    @Test
    void aGuestWhoCreatesAnEventIsMailedTheirCodeByTheNextDrain() {
        Event created = api.createDatedEvent("Kuba", now.plusDays(7), null, false);

        assertThat(mailSender.sent()).as("nothing runs after the response on Cloud Run").isEmpty();

        scheduledJobDrainer.drainDue(now);

        assertThat(mailSender.sent()).singleElement().satisfies(mail -> {
            assertThat(mail.to()).isEqualTo(EventApi.emailOf("Kuba"));
            assertThat(mail.subject()).startsWith("Twój PIN").contains("Barbecue");
            assertThat(mail.body()).contains(">" + created.host().returnCode() + "<").contains("Kuba");
        });
        assertThat(deliveriesOf(EmailDeliveryStatus.SENT)).isEqualTo(1);
    }

    @Test
    void aUniteAccountCreatingAnEventHasNoCodeToSend() throws Exception {
        AppUserRecord user = users.getNewUser().withRandomValues().buildAndSave();
        CreateEventRequest request = new CreateEventRequest("Barbecue", null, now.plusDays(7), null, null, null,
                null, false, null, null, null, null, null, null, null, false);

        mvc.perform(post("/public/event")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.generateTokenForUser(user, UserRole.RESIDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        scheduledJobDrainer.drainDue(now);

        assertThat(mailSender.sent()).isEmpty();
    }

    @Test
    void aGuestWhoJoinsIsMailedTheirCodeByTheNextDrain() {
        Person anna = api.join(event, "Anna");

        assertThat(mailSender.sent()).as("nothing runs after the response on Cloud Run").isEmpty();

        scheduledJobDrainer.drainDue(now);

        assertThat(mailSender.sent()).singleElement().satisfies(mail -> {
            assertThat(mail.to()).isEqualTo(EventApi.emailOf("Anna"));
            assertThat(mail.subject()).startsWith("Twój PIN").contains("Barbecue");
            assertThat(mail.body()).contains(">" + anna.returnCode() + "<").contains("Anna");
        });
        assertThat(deliveriesOf(EmailDeliveryStatus.SENT)).isEqualTo(1);
    }

    @Test
    void twoGuestsSharingAnAddressEachGetTheirOwnCode() throws Exception {
        String anna = joinWithAddress("Anna", "family@test.local");
        String ben = joinWithAddress("Ben", "family@test.local");

        scheduledJobDrainer.drainDue(now);

        List<MailMessage> sent = mailSender.sent();
        assertThat(sent).hasSize(2).allSatisfy(mail -> assertThat(mail.to()).isEqualTo("family@test.local"));
        assertThat(sent).anySatisfy(mail -> assertThat(mail.body()).contains(">" + anna + "<"));
        assertThat(sent).anySatisfy(mail -> assertThat(mail.body()).contains(">" + ben + "<"));
    }

    @Test
    void comingBackDoesNotSendTheCodeAgain() {
        Person anna = api.join(event, "Anna");
        scheduledJobDrainer.drainDue(now);
        mailSender.clear();

        api.resumeWithCode(event, "Anna", anna.returnCode());
        scheduledJobDrainer.drainDue(now);

        assertThat(mailSender.sent()).isEmpty();
    }

    @Test
    void aUniteAccountHasNoCodeToSend() {
        AppUserRecord user = users.getNewUser().withRandomValues().buildAndSave();

        api.joinAsUniteUser(event, jwt.generateTokenForUser(user, UserRole.RESIDENT));
        scheduledJobDrainer.drainDue(now);

        assertThat(mailSender.sent()).isEmpty();
    }

    @Test
    void aMailThatFailsIsTriedAgainWithoutUndoingTheJoin() {
        mailSender.failFor(EventApi.emailOf("Anna"));

        Person anna = api.join(event, "Anna");
        scheduledJobDrainer.drainDue(now);

        assertThat(mailSender.sent()).isEmpty();
        assertThat(deliveriesOf(EmailDeliveryStatus.FAILED)).isEqualTo(1);

        mailSender.failFor();
        scheduledJobDrainer.drainDue(now.plusHours(1));

        assertThat(mailSender.sent()).singleElement()
                .satisfies(mail -> assertThat(mail.body()).contains(">" + anna.returnCode() + "<"));
        assertThat(deliveriesOf(EmailDeliveryStatus.SENT)).isEqualTo(1);
    }

    @Test
    void aMemberRemovedBeforeTheDrainIsNotWrittenTo() {
        api.join(event, "Anna");
        dslContext.deleteFrom(EVENT_MEMBER).where(EVENT_MEMBER.DISPLAY_NAME.eq("Anna")).execute();

        scheduledJobDrainer.drainDue(now);

        assertThat(mailSender.sent()).isEmpty();
    }

    private String joinWithAddress(String name, String address) throws Exception {
        String body = mvc.perform(post("/public/event/" + event.slug() + "/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OpenSessionRequest(name, address, null, null, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("returnCode").asText();
    }

    private int deliveriesOf(EmailDeliveryStatus status) {
        return dslContext.fetchCount(EMAIL_DELIVERY, EMAIL_DELIVERY.DELIVERY_TYPE.eq(EmailDeliveryType.EVENT_MEMBER_CREATED.name())
                .and(EMAIL_DELIVERY.STATUS.eq(status.name())));
    }
}
