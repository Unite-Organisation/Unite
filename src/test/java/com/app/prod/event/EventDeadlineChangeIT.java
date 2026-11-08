package com.app.prod.event;

import com.app.prod.config.IntegrationTest;
import com.app.prod.config.MutableClock;
import com.app.prod.event.EventApi.Event;
import com.app.prod.event.EventApi.Person;
import com.app.prod.event.dto.SlotRequest;
import com.app.prod.event.dto.UpdateEventRequest;
import com.app.prod.event.enums.EventStatus;
import com.app.prod.job.enums.JobStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.jooq.Record2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.app.prod.event.EventApi.suits;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.sources.Tables.EVENT;
import static org.jooq.sources.Tables.SCHEDULED_JOB;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The deadline is booked as a job when the event is created, so moving it means rebooking that job -
 * and the old one must not close the voting at the time the host no longer wants.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EventDeadlineChangeIT extends IntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private DSLContext dslContext;
    @Autowired
    private MutableClock clock;

    private EventApi api;
    private LocalDateTime now;
    private LocalDateTime deadline;
    private List<SlotRequest> dates;

    @BeforeEach
    void setUp() throws Exception {
        dslContext.deleteFrom(EVENT).execute();
        dslContext.deleteFrom(SCHEDULED_JOB).execute();

        clock.setInstant(Instant.now().truncatedTo(ChronoUnit.MICROS));
        api = new EventApi(mvc, objectMapper);
        now = LocalDateTime.now(clock);
        deadline = now.plusDays(2);
        dates = List.of(
                new SlotRequest(now.plusDays(7), null),
                new SlotRequest(now.plusDays(8), null)
        );
    }

    @Test
    void anOldDeadlineNoLongerClosesTheVotingOnceTheHostMovedIt() throws Exception {
        Event event = api.createEventChoosingItsDate("Kuba", 3, deadline, dates);
        LocalDateTime later = deadline.plusDays(1);

        moveDeadline(event, later);

        assertThat(LocalDateTime.parse(api.event(event).get("votingDeadline").asText())).isEqualTo(later);
        assertThat(deadlineJobs())
                .as("the old job is called off, the new one waits for the new deadline")
                .extracting(job -> job.value1(), job -> job.value2())
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(deadline, JobStatus.CANCELLED.name()),
                        org.assertj.core.groups.Tuple.tuple(later, JobStatus.PENDING.name()));

        clock.setInstant(instantOf(deadline.plusMinutes(1)));
        api.runDueJobs();
        assertThat(api.statusOf(event)).isEqualTo(EventStatus.COLLECTING_VOTES.name());

        clock.setInstant(instantOf(later.plusMinutes(1)));
        api.runDueJobs();
        assertThat(api.statusOf(event)).isEqualTo(EventStatus.ONE_SLOT_LEFT.name());
    }

    @Test
    void anEarlierDeadlineClosesTheVotingEarlier() throws Exception {
        Event event = api.createEventChoosingItsDate("Kuba", 3, deadline, dates);
        LocalDateTime earlier = now.plusHours(1);

        moveDeadline(event, earlier);

        clock.setInstant(instantOf(earlier.plusMinutes(1)));
        api.runDueJobs();
        assertThat(api.statusOf(event)).isEqualTo(EventStatus.ONE_SLOT_LEFT.name());
    }

    @Test
    void theDeadlineIsLockedInTheLastFiveMinutes() throws Exception {
        Event event = api.createEventChoosingItsDate("Kuba", 3, deadline, dates);
        clock.setInstant(instantOf(deadline.minusMinutes(4)));

        api.tryUpdate(event, event.host(), deadlineOnly(deadline.plusDays(1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("EVENT_DEADLINE_LOCKED"))
                .andExpect(jsonPath("$.errors[0].field").value("votingDeadline"));
    }

    @Test
    void theNewDeadlineHasToLeaveAtLeastFiveMinutes() throws Exception {
        Event event = api.createEventChoosingItsDate("Kuba", 3, deadline, dates);

        api.tryUpdate(event, event.host(), deadlineOnly(now.plusMinutes(4)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_TIME_PERIOD"))
                .andExpect(jsonPath("$.errors[0].field").value("votingDeadline"));
        api.tryUpdate(event, event.host(), deadlineOnly(now.minusDays(1)))
                .andExpect(status().isBadRequest());

        assertThat(LocalDateTime.parse(api.event(event).get("votingDeadline").asText())).isEqualTo(deadline);
    }

    @Test
    void theDeadlineCannotPassAProposedDate() throws Exception {
        Event event = api.createEventChoosingItsDate("Kuba", 3, deadline, dates);

        api.tryUpdate(event, event.host(), deadlineOnly(now.plusDays(7)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_TIME_PERIOD"));
    }

    @Test
    void theDeadlineCannotMoveOnceTheGroupIsTogether() throws Exception {
        Event event = api.createEventChoosingItsDate("Kuba", 2, deadline, dates);
        api.join(event, "Anna", suits(api.datesInOrder(event).getFirst()));
        assertThat(api.statusOf(event)).isEqualTo(EventStatus.GROUP_FORMED.name());

        api.tryUpdate(event, event.host(), deadlineOnly(deadline.plusDays(1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("EVENT_DEADLINE_LOCKED"));
    }

    @Test
    void sendingTheSameDeadlineBackChangesNothing() throws Exception {
        Event event = api.createEventChoosingItsDate("Kuba", 3, deadline, dates);
        clock.setInstant(instantOf(deadline.minusMinutes(1)));

        api.tryUpdate(event, event.host(), new UpdateEventRequest("Siatkówka", null, null, null, null, deadline, null))
                .andExpect(status().isOk());

        assertThat(api.event(event).get("name").asText()).isEqualTo("Siatkówka");
        assertThat(deadlineJobs()).extracting(job -> job.value2()).containsExactly(JobStatus.PENDING.name());
    }

    @Test
    void anEventWithADateHasNoDeadlineToMove() throws Exception {
        Event event = api.createDatedEvent("Kuba", now.plusDays(7), null, false);

        api.tryUpdate(event, event.host(), deadlineOnly(now.plusDays(1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("EVENT_MODE_MISMATCH"));
    }

    @Test
    void onlyTheHostMovesTheDeadline() throws Exception {
        Event event = api.createEventChoosingItsDate("Kuba", 3, deadline, dates);
        Person anna = api.join(event, "Anna", suits(api.datesInOrder(event).getFirst()));

        api.tryUpdate(event, anna, deadlineOnly(deadline.plusDays(1))).andExpect(status().isForbidden());
    }

    private void moveDeadline(Event event, LocalDateTime votingDeadline) {
        try {
            api.tryUpdate(event, event.host(), deadlineOnly(votingDeadline)).andExpect(status().isOk());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static UpdateEventRequest deadlineOnly(LocalDateTime votingDeadline) {
        return new UpdateEventRequest(null, null, null, null, null, votingDeadline, null);
    }

    private List<Record2<LocalDateTime, String>> deadlineJobs() {
        return dslContext.select(SCHEDULED_JOB.RUN_AT, SCHEDULED_JOB.STATUS)
                .from(SCHEDULED_JOB)
                .where(SCHEDULED_JOB.DEDUPE_KEY.like("event_voting_deadline:%"))
                .fetch();
    }

    private Instant instantOf(LocalDateTime time) {
        return time.atZone(clock.getZone()).toInstant();
    }
}
