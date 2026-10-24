package com.app.prod.event;

import com.app.prod.config.IntegrationTest;
import com.app.prod.config.MutableClock;
import com.app.prod.event.EventApi.Event;
import com.app.prod.event.EventApi.Person;
import com.app.prod.event.dto.SlotRequest;
import com.app.prod.event.dto.UpdateEventRequest;
import com.app.prod.event.enums.EventMemberStatus;
import com.app.prod.event.enums.EventStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
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
import java.util.UUID;

import static com.app.prod.event.EventApi.suits;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.sources.Tables.EVENT;
import static org.jooq.sources.Tables.SCHEDULED_JOB;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The link to an event is public and stays the same for its whole life, so the way to keep someone
 * out is not a new link but a host who lets newcomers in one by one.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EventMemberApprovalIT extends IntegrationTest {

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

    @BeforeEach
    void setUp() {
        dslContext.deleteFrom(EVENT).execute();
        dslContext.deleteFrom(SCHEDULED_JOB).execute();

        clock.setInstant(Instant.now().truncatedTo(ChronoUnit.MICROS));
        api = new EventApi(mvc, objectMapper);
        now = LocalDateTime.now(clock);
    }

    /** The clock stands still in here, and the queue is told apart by when people asked. */
    private Person askToJoin(Event event, String name) {
        clock.advance(Duration.ofSeconds(1));
        return api.askToJoin(event, name);
    }

    @Test
    void theHostThrowsSomeoneOutAndFromThenOnLetsPeopleInThemselves() throws Exception {
        Event event = api.createDatedEvent("Kuba", now.plusDays(7), null, false);
        api.signUp(event, "Anna");
        Person troll = api.signUp(event, "Troll");

        api.remove(event, "Troll");

        assertThat(api.namesWithStatus(event, EventMemberStatus.GOING)).containsExactlyInAnyOrder("Kuba", "Anna");
        assertThat(api.eventSeenBy(event, troll).get("me").isNull())
                .as("being thrown out ends the session too, not only the membership")
                .isTrue();

        api.requireApproval(event);
        assertThat(api.event(event).get("requiresApproval").asBoolean()).isTrue();

        Person trollAgain = askToJoin(event, "Troll");
        assertThat(api.eventSeenBy(event, trollAgain).get("me").get("status").asText())
                .isEqualTo(EventMemberStatus.PENDING_APPROVAL.name());
        api.trySay(event, trollAgain, EventMemberStatus.GOING).andExpect(status().isForbidden());
        assertThat(api.namesWithStatus(event, EventMemberStatus.GOING)).containsExactlyInAnyOrder("Kuba", "Anna");

        Person bartek = askToJoin(event, "Bartek");
        assertThat(api.awaitingApproval(event)).containsExactly("Troll", "Bartek");

        // the troll is first in line, so the host has to deal with them before getting to Bartek
        api.tryApprove(event, event.host(), api.idsOf(event, "Bartek")).andExpect(status().isBadRequest());
        api.remove(event, "Troll");
        api.approve(event, "Bartek");

        assertThat(api.namesWithStatus(event, EventMemberStatus.GOING)).containsExactlyInAnyOrder("Kuba", "Anna", "Bartek");
        assertThat(api.awaitingApproval(event)).isEmpty();
        assertThat(api.eventSeenBy(event, bartek).get("me").get("status").asText()).isEqualTo(EventMemberStatus.GOING.name());
    }

    @Test
    void theHostChoosesHowManyToLetInButNeverWhoGoesFirst() throws Exception {
        Event event = api.createDatedEvent("Kuba", now.plusDays(7), null, false, true);
        askToJoin(event, "Anna");
        askToJoin(event, "Bartek");
        askToJoin(event, "Celina");

        assertThat(api.awaitingApproval(event)).containsExactly("Anna", "Bartek", "Celina");

        api.tryApprove(event, event.host(), api.idsOf(event, "Bartek", "Celina")).andExpect(status().isBadRequest());
        api.tryApprove(event, event.host(), api.idsOf(event, "Anna", "Celina")).andExpect(status().isBadRequest());

        api.approve(event, "Bartek", "Anna");

        assertThat(api.namesWithStatus(event, EventMemberStatus.GOING)).containsExactlyInAnyOrder("Kuba", "Anna", "Bartek");
        assertThat(api.awaitingApproval(event)).containsExactly("Celina");

        api.tryApprove(event, event.host(), api.idsOf(event, "Anna"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("EVENT_MEMBER_NOT_AWAITING_APPROVAL"));
    }

    @Test
    void seatsGoToTheApprovedInTheOrderTheyAsked() {
        Event event = api.createDatedEvent("Kuba", now.plusDays(7), 2, true, true);
        askToJoin(event, "Anna");
        askToJoin(event, "Bartek");
        askToJoin(event, "Celina");

        api.approve(event, "Celina", "Bartek", "Anna");

        assertThat(api.namesWithStatus(event, EventMemberStatus.GOING)).containsExactlyInAnyOrder("Kuba", "Anna");
        assertThat(api.namesWithStatus(event, EventMemberStatus.WAITLIST)).containsExactlyInAnyOrder("Bartek", "Celina");

        // approved together, so they joined the waitlist at the same moment - the queue still decides
        api.remove(event, "Anna");

        assertThat(api.namesWithStatus(event, EventMemberStatus.GOING)).containsExactlyInAnyOrder("Kuba", "Bartek");
        assertThat(api.namesWithStatus(event, EventMemberStatus.WAITLIST)).containsExactly("Celina");
    }

    @Test
    void choicesOfPeopleNotLetInYetDoNotFormTheGroup() {
        List<SlotRequest> dates = List.of(
                new SlotRequest(now.plusDays(7), null),
                new SlotRequest(now.plusDays(8), null)
        );
        Event event = api.createEventChoosingItsDateRequiringApproval("Kuba", 3, now.plusDays(2), dates);
        UUID first = api.datesInOrder(event).getFirst();

        api.join(event, "Anna", suits(first));
        clock.advance(Duration.ofSeconds(1));
        api.join(event, "Bartek", suits(first));

        assertThat(api.statusOf(event)).isEqualTo(EventStatus.COLLECTING_VOTES.name());
        assertThat(api.date(event, first).get("preferredCount").asInt()).isEqualTo(1);

        api.approve(event, "Anna", "Bartek");

        assertThat(api.statusOf(event)).isEqualTo(EventStatus.GROUP_FORMED.name());
        assertThat(api.namesWithStatus(event, EventMemberStatus.GOING)).containsExactlyInAnyOrder("Kuba", "Anna", "Bartek");
    }

    @Test
    void throwingOutAMemberOfAFormedGroupCanBreakItUp() {
        List<SlotRequest> dates = List.of(
                new SlotRequest(now.plusDays(7), null),
                new SlotRequest(now.plusDays(8), null)
        );
        Event event = api.createEventChoosingItsDate("Kuba", 2, now.plusDays(2), dates);
        api.join(event, "Troll", suits(api.datesInOrder(event).getFirst()));
        assertThat(api.statusOf(event)).isEqualTo(EventStatus.GROUP_FORMED.name());

        api.remove(event, "Troll");

        assertThat(api.statusOf(event)).isEqualTo(EventStatus.COLLECTING_VOTES.name());
    }

    @Test
    void onlyTheHostModeratesAndTheHostStays() throws Exception {
        Event event = api.createDatedEvent("Kuba", now.plusDays(7), null, false);
        Person anna = api.signUp(event, "Anna");
        List<UUID> host = api.idsOf(event, "Kuba");

        api.tryRemove(event, anna, host).andExpect(status().isForbidden());
        api.tryApprove(event, anna, api.idsOf(event, "Anna")).andExpect(status().isForbidden());
        api.tryUpdate(event, anna, new UpdateEventRequest("Mine now", null, null, null, null)).andExpect(status().isForbidden());

        api.tryRemove(event, event.host(), host)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("EVENT_HOST_CANNOT_BE_REMOVED"));
        api.tryRemove(event, event.host(), List.of(UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void approvalOnceSwitchedOnStaysOn() throws Exception {
        Event event = api.createDatedEvent("Kuba", now.plusDays(7), null, false);
        api.requireApproval(event);

        api.tryUpdate(event, event.host(), new UpdateEventRequest(null, null, null, null, false))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("EVENT_APPROVAL_CANNOT_BE_DISABLED"));
        assertThat(api.event(event).get("requiresApproval").asBoolean()).isTrue();
    }

    @Test
    void theHostEditsWhatTheEventSays() throws Exception {
        Event event = api.createDatedEvent("Kuba", now.plusDays(7), null, false);

        api.tryUpdate(event, event.host(), new UpdateEventRequest("Grill nad Wisłą", "Bring sausages", "Bulwary", null, null))
                .andExpect(status().isOk());
        api.tryUpdate(event, event.host(), new UpdateEventRequest(null, "", null, null, null))
                .andExpect(status().isOk());
        api.tryUpdate(event, event.host(), new UpdateEventRequest(" ", null, null, null, null))
                .andExpect(status().isBadRequest());

        JsonNode seen = api.event(event);
        assertThat(seen.get("name").asText()).isEqualTo("Grill nad Wisłą");
        assertThat(seen.get("description").isNull()).as("an empty value clears the field").isTrue();
        assertThat(seen.get("locationName").asText()).isEqualTo("Bulwary");
        assertThat(seen.get("requiresApproval").asBoolean()).isFalse();
    }
}
