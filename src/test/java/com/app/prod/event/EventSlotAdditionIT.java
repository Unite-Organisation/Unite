package com.app.prod.event;

import com.app.prod.config.IntegrationTest;
import com.app.prod.config.MutableClock;
import com.app.prod.event.EventApi.Event;
import com.app.prod.event.EventApi.Person;
import com.app.prod.event.dto.SlotRequest;
import com.app.prod.event.dto.UpdateEventRequest;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.app.prod.event.EventApi.onlyIfNeeded;
import static com.app.prod.event.EventApi.suits;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.sources.Tables.EVENT;
import static org.jooq.sources.Tables.EVENT_SLOT_VOTE;
import static org.jooq.sources.Tables.SCHEDULED_JOB;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The host can put more dates on the table while the group is voting, up to the limit - but the
 * dates already there stay, because people have voted on them.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EventSlotAdditionIT extends IntegrationTest {

    private static final int MAX_SLOTS = 8;

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
    private List<SlotRequest> twoDates;

    @BeforeEach
    void setUp() {
        dslContext.deleteFrom(EVENT).execute();
        dslContext.deleteFrom(SCHEDULED_JOB).execute();

        clock.setInstant(Instant.now().truncatedTo(ChronoUnit.MICROS));
        api = new EventApi(mvc, objectMapper);
        now = LocalDateTime.now(clock);
        deadline = now.plusDays(2);
        twoDates = List.of(
                new SlotRequest(now.plusDays(7), null),
                new SlotRequest(now.plusDays(8), null)
        );
    }

    @Test
    void theNewDateJoinsTheVoteWithTheHostOnIt() throws Exception {
        Event event = api.createEventChoosingItsDate("Kuba", 3, deadline, twoDates);
        Person anna = api.join(event, "Anna", suits(api.datesInOrder(event).getFirst()));
        LocalDateTime friday = now.plusDays(9);

        api.tryUpdate(event, event.host(), adding(new SlotRequest(friday, null))).andExpect(status().isOk());

        List<JsonNode> slots = slotsOf(event);
        assertThat(slots).hasSize(3);
        JsonNode added = slots.stream()
                .filter(slot -> LocalDateTime.parse(slot.get("startDate").asText()).equals(friday))
                .findFirst().orElseThrow();
        assertThat(added.get("preferredCount").asInt()).as("the host suits what they propose").isEqualTo(1);
        assertThat(added.get("open").asBoolean()).isTrue();

        assertThat(api.eventSeenBy(event, anna).get("myVotes"))
                .as("nobody else is put down for a date they have not seen")
                .hasSize(1);
        assertThat(api.eventSeenBy(event, event.host()).get("myVotes"))
                .as("the host keeps the votes they had")
                .hasSize(3);
    }

    @Test
    void theHostKeepsTheirPlaceInTheQueueOnTheDatesAlreadyThere() throws Exception {
        Event event = api.createEventChoosingItsDate("Kuba", 3, deadline, twoDates);
        UUID monday = api.datesInOrder(event).getFirst();
        LocalDateTime votedAt = voteTimeOn(monday);

        clock.advance(Duration.ofHours(1));
        api.tryUpdate(event, event.host(), adding(new SlotRequest(now.plusDays(9), null))).andExpect(status().isOk());

        assertThat(voteTimeOn(monday))
                .as("seats go to whoever voted first, so adding a date must not move the host to the back")
                .isEqualTo(votedAt);
    }

    @Test
    void theHostsOwnChoiceOnTheOtherDatesStays() throws Exception {
        Event event = api.createEventChoosingItsDate("Kuba", 3, deadline, twoDates);
        List<UUID> dates = api.datesInOrder(event);
        api.changeChoice(event, event.host(), onlyIfNeeded(dates.getFirst()));

        api.tryUpdate(event, event.host(), adding(new SlotRequest(now.plusDays(9), null))).andExpect(status().isOk());

        JsonNode votes = api.eventSeenBy(event, event.host()).get("myVotes");
        assertThat(votes).hasSize(2);
        assertThat(votes).anySatisfy(vote -> {
            assertThat(vote.get("slotId").asText()).isEqualTo(dates.getFirst().toString());
            assertThat(vote.get("preference").asText()).isEqualTo("IF_NEEDED");
        });
        assertThat(votes).noneSatisfy(vote ->
                assertThat(vote.get("slotId").asText()).as("a date the host dropped stays dropped").isEqualTo(dates.get(1).toString()));
    }

    @Test
    void noMoreThanTheLimit() throws Exception {
        List<SlotRequest> oneShort = IntStream.range(0, MAX_SLOTS - 1)
                .mapToObj(day -> new SlotRequest(now.plusDays(7 + day), null))
                .toList();
        Event event = api.createEventChoosingItsDate("Kuba", 3, deadline, oneShort);

        api.tryUpdate(event, event.host(), adding(new SlotRequest(now.plusDays(20), null), new SlotRequest(now.plusDays(21), null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("addSlots"));
        api.tryUpdate(event, event.host(), adding(new SlotRequest(now.plusDays(20), null)))
                .andExpect(status().isOk());

        assertThat(slotsOf(event)).hasSize(MAX_SLOTS);
    }

    @Test
    void aDateAlreadyOnTheTableIsNotProposedTwice() throws Exception {
        Event event = api.createEventChoosingItsDate("Kuba", 3, deadline, twoDates);

        api.tryUpdate(event, event.host(), adding(twoDates.getFirst()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("addSlots[0].startDate"));
    }

    @Test
    void theNewDateHasToFallAfterTheDeadlineAsItWillBe() throws Exception {
        Event event = api.createEventChoosingItsDate("Kuba", 3, deadline, twoDates);
        LocalDateTime laterDeadline = deadline.plusDays(1);
        SlotRequest betweenDeadlines = new SlotRequest(deadline.plusHours(12), null);

        api.tryUpdate(event, event.host(), adding(new SlotRequest(deadline.minusHours(1), null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_TIME_PERIOD"))
                .andExpect(jsonPath("$.errors[0].field").value("addSlots[0].startDate"));

        api.tryUpdate(event, event.host(), new UpdateEventRequest(null, null, null, null, null, laterDeadline, List.of(betweenDeadlines)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("addSlots[0].startDate"));

        assertThat(LocalDateTime.parse(api.event(event).get("votingDeadline").asText()))
                .as("a rejected request changes nothing, the deadline included")
                .isEqualTo(deadline);
        assertThat(slotsOf(event)).hasSize(2);
    }

    @Test
    void datesAreLockedInTheLastFiveMinutes() throws Exception {
        Event event = api.createEventChoosingItsDate("Kuba", 3, deadline, twoDates);
        clock.setInstant(deadline.minusMinutes(4).atZone(clock.getZone()).toInstant());

        api.tryUpdate(event, event.host(), adding(new SlotRequest(now.plusDays(9), null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("EVENT_DEADLINE_LOCKED"))
                .andExpect(jsonPath("$.errors[0].field").value("addSlots"));
    }

    @Test
    void noDatesOnceTheGroupIsTogether() throws Exception {
        Event event = api.createEventChoosingItsDate("Kuba", 2, deadline, twoDates);
        api.join(event, "Anna", suits(api.datesInOrder(event).getFirst()));

        api.tryUpdate(event, event.host(), adding(new SlotRequest(now.plusDays(9), null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("EVENT_DEADLINE_LOCKED"));
    }

    @Test
    void anEventWithADateTakesNoDates() throws Exception {
        Event event = api.createDatedEvent("Kuba", now.plusDays(7), null, false);

        api.tryUpdate(event, event.host(), adding(new SlotRequest(now.plusDays(9), null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("EVENT_MODE_MISMATCH"));
    }

    @Test
    void onlyTheHostAddsDates() throws Exception {
        Event event = api.createEventChoosingItsDate("Kuba", 3, deadline, twoDates);
        Person anna = api.join(event, "Anna", suits(api.datesInOrder(event).getFirst()));

        api.tryUpdate(event, anna, adding(new SlotRequest(now.plusDays(9), null))).andExpect(status().isForbidden());
    }

    /** Only the host votes in these events, so a date carries exactly one vote. */
    private LocalDateTime voteTimeOn(UUID slotId) {
        return dslContext.select(EVENT_SLOT_VOTE.CREATED_AT).from(EVENT_SLOT_VOTE)
                .where(EVENT_SLOT_VOTE.SLOT_ID.eq(slotId))
                .fetchSingle(EVENT_SLOT_VOTE.CREATED_AT);
    }

    private static UpdateEventRequest adding(SlotRequest... slots) {
        return new UpdateEventRequest(null, null, null, null, null, null, List.of(slots));
    }

    private List<JsonNode> slotsOf(Event event) {
        List<JsonNode> slots = new ArrayList<>();
        api.event(event).get("slots").forEach(slots::add);
        return slots;
    }
}
