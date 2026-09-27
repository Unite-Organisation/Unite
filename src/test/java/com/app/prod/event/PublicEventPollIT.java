package com.app.prod.event;

import com.app.prod.config.IntegrationTest;
import com.app.prod.config.MutableClock;
import com.app.prod.event.dto.CreateEventRequest;
import com.app.prod.event.dto.OpenSessionRequest;
import com.app.prod.event.dto.SlotRequest;
import com.app.prod.event.dto.SlotVoteRequest;
import com.app.prod.event.dto.VotesRequest;
import com.app.prod.event.enums.EventStatus;
import com.app.prod.event.enums.SchedulingMode;
import com.app.prod.event.enums.SlotPreference;
import com.app.prod.event.service.EventSessionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.sources.Tables.EVENT;
import static org.jooq.sources.Tables.SCHEDULED_JOB;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PublicEventPollIT extends IntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private DSLContext dslContext;
    @Autowired
    private MutableClock clock;

    private LocalDateTime now;
    private LocalDateTime deadline;
    private List<SlotRequest> fourDates;

    @BeforeEach
    void setUp() {
        dslContext.deleteFrom(EVENT).execute();
        dslContext.deleteFrom(SCHEDULED_JOB).execute();

        clock.setInstant(Instant.now().truncatedTo(ChronoUnit.MICROS));
        now = LocalDateTime.now(clock);
        deadline = now.plusDays(2);
        fourDates = List.of(
                new SlotRequest(now.plusDays(7), now.plusDays(7).plusHours(2)),
                new SlotRequest(now.plusDays(8), null),
                new SlotRequest(now.plusDays(9), null),
                new SlotRequest(now.plusDays(10), null)
        );
    }

    @Test
    void shouldCreateAnEventThatIsStillChoosingItsDate() throws Exception {
        Created event = createPollEvent(3);

        JsonNode body = getEvent(event);

        assertThat(body.get("schedulingMode").asText()).isEqualTo(SchedulingMode.POLL.name());
        assertThat(body.get("status").asText()).isEqualTo(EventStatus.COLLECTING_VOTES.name());
        assertThat(body.get("startDate").isNull()).as("the date is what the group decides").isTrue();
        assertThat(body.get("slots")).hasSize(4);
    }

    @Test
    void shouldCountTheHostOnEveryDateTheyProposed() throws Exception {
        Created event = createPollEvent(3);

        JsonNode slots = getEvent(event).get("slots");

        assertThat(slots).allSatisfy(slot ->
                assertThat(slot.get("preferredCount").asInt())
                        .as("the host put these dates up, so all of them suit them")
                        .isEqualTo(1));
    }

    @Test
    void shouldBookTheDeadlineAsAJob() throws Exception {
        createPollEvent(3);

        LocalDateTime runAt = dslContext.select(SCHEDULED_JOB.RUN_AT).from(SCHEDULED_JOB)
                .fetchOne(SCHEDULED_JOB.RUN_AT);

        assertThat(runAt).isEqualTo(deadline);
    }

    @Test
    void shouldRejectAnEventWithoutAThresholdOrADeadline() throws Exception {
        CreateEventRequest request = new CreateEventRequest("Volleyball", null, null, null, null, null,
                null, false, "Kuba", null, SchedulingMode.POLL, null, null, fourDates);

        mvc.perform(json(post("/public/event"), request)).andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectAnEventThatBothProposesDatesAndHasOne() throws Exception {
        CreateEventRequest request = new CreateEventRequest("Volleyball", null, now.plusDays(7), null, null, null,
                null, false, "Kuba", null, SchedulingMode.POLL, 3, deadline, fourDates);

        mvc.perform(json(post("/public/event"), request)).andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectASingleProposedDate() throws Exception {
        CreateEventRequest request = new CreateEventRequest("Volleyball", null, null, null, null, null,
                null, false, "Kuba", null, SchedulingMode.POLL, 3, deadline,
                List.of(new SlotRequest(now.plusDays(7), null)));

        mvc.perform(json(post("/public/event"), request)).andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectADateThatFallsBeforeTheDeadlineForChoosingIt() throws Exception {
        CreateEventRequest request = new CreateEventRequest("Volleyball", null, null, null, null, null,
                null, false, "Kuba", null, SchedulingMode.POLL, 3, deadline,
                List.of(new SlotRequest(now.plusDays(1), null), new SlotRequest(now.plusDays(7), null)));

        mvc.perform(json(post("/public/event"), request)).andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectProposedDatesOnAnEventThatAlreadyHasOne() throws Exception {
        CreateEventRequest request = new CreateEventRequest("Volleyball", null, now.plusDays(7), null, null, null,
                null, false, "Kuba", null, null, null, null, fourDates);

        mvc.perform(json(post("/public/event"), request)).andExpect(status().isBadRequest());
    }

    @Test
    void shouldLetAGuestJoinByNamingTheDatesThatWork() throws Exception {
        Created event = createPollEvent(3);
        UUID monday = slotId(event, 0);

        join(event, "Anna", List.of(new SlotVoteRequest(monday, SlotPreference.PREFERRED)))
                .andExpect(status().isCreated());

        JsonNode slot = slotById(getEvent(event), monday);
        assertThat(slot.get("preferredCount").asInt()).isEqualTo(2);
    }

    @Test
    void shouldRefuseToJoinWithoutNamingASingleDate() throws Exception {
        Created event = createPollEvent(3);

        join(event, "Anna", List.of()).andExpect(status().isBadRequest());
    }

    @Test
    void shouldRefuseAVoteForAnotherEventsDate() throws Exception {
        Created event = createPollEvent(3);
        Created other = createPollEvent(3);

        join(event, "Anna", List.of(new SlotVoteRequest(slotId(other, 0), SlotPreference.PREFERRED)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReplaceTheWholeChoiceRatherThanAddingToIt() throws Exception {
        Created event = createPollEvent(4);
        UUID monday = slotId(event, 0);
        UUID tuesday = slotId(event, 1);

        Cookie anna = joinAndKeepSession(event, "Anna", List.of(new SlotVoteRequest(monday, SlotPreference.PREFERRED)));

        mvc.perform(json(put("/public/event/" + event.slug + "/votes"), new VotesRequest(
                        List.of(new SlotVoteRequest(tuesday, SlotPreference.IF_NEEDED)))).cookie(anna))
                .andExpect(status().isOk());

        JsonNode body = getEvent(event);
        assertThat(slotById(body, monday).get("preferredCount").asInt()).as("monday was dropped").isEqualTo(1);
        assertThat(slotById(body, tuesday).get("ifNeededCount").asInt()).isEqualTo(1);
    }

    @Test
    void shouldFormTheGroupOnceADateHasEnoughPeople() throws Exception {
        Created event = createPollEvent(3);
        UUID monday = slotId(event, 0);

        join(event, "Anna", List.of(new SlotVoteRequest(monday, SlotPreference.PREFERRED)));
        join(event, "Bartek", List.of(new SlotVoteRequest(monday, SlotPreference.PREFERRED)));

        JsonNode body = getEvent(event);
        assertThat(body.get("status").asText()).isEqualTo(EventStatus.GROUP_FORMED.name());
        assertThat(body.get("selectedSlotId").asText()).isEqualTo(monday.toString());
    }

    @Test
    void shouldCloseTheOtherDatesOnceTheGroupIsFormed() throws Exception {
        Created event = createPollEvent(3);
        UUID monday = slotId(event, 0);

        join(event, "Anna", List.of(new SlotVoteRequest(monday, SlotPreference.PREFERRED)));
        join(event, "Bartek", List.of(new SlotVoteRequest(monday, SlotPreference.PREFERRED)));

        assertThat(getEvent(event).get("slots")).allSatisfy(slot ->
                assertThat(slot.get("open").asBoolean()).isFalse());

        join(event, "Celina", List.of(new SlotVoteRequest(slotId(event, 1), SlotPreference.PREFERRED)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldLetTheHostStartTheEventWithoutWaitingOutTheWindow() throws Exception {
        Created event = createPollEvent(3);
        UUID monday = slotId(event, 0);
        join(event, "Anna", List.of(new SlotVoteRequest(monday, SlotPreference.PREFERRED)));
        join(event, "Bartek", List.of(new SlotVoteRequest(monday, SlotPreference.PREFERRED)));

        mvc.perform(post("/public/event/" + event.slug + "/confirm").cookie(event.session))
                .andExpect(status().isOk());

        JsonNode body = getEvent(event);
        assertThat(body.get("status").asText()).isEqualTo(EventStatus.CONFIRMED.name());
        assertThat(body.get("startDate").isNull())
                .as("a confirmed event becomes an ordinary dated event")
                .isFalse();
    }

    @Test
    void shouldNotLetAMemberStartTheEvent() throws Exception {
        Created event = createPollEvent(3);
        UUID monday = slotId(event, 0);
        Cookie anna = joinAndKeepSession(event, "Anna", List.of(new SlotVoteRequest(monday, SlotPreference.PREFERRED)));
        join(event, "Bartek", List.of(new SlotVoteRequest(monday, SlotPreference.PREFERRED)));

        mvc.perform(post("/public/event/" + event.slug + "/confirm").cookie(anna))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldShowTheFullestDateFirst() throws Exception {
        Created event = createPollEvent(5);
        UUID wednesday = slotId(event, 2);

        join(event, "Anna", List.of(new SlotVoteRequest(wednesday, SlotPreference.PREFERRED)));
        join(event, "Bartek", List.of(new SlotVoteRequest(wednesday, SlotPreference.PREFERRED)));

        JsonNode slots = getEvent(event).get("slots");
        assertThat(slots.get(0).get("id").asText()).isEqualTo(wednesday.toString());
        assertThat(slots.get(0).get("chance").asInt()).isEqualTo(60);
    }

    @Test
    void shouldStillCreateAnOrdinaryDatedEvent() throws Exception {
        CreateEventRequest request = new CreateEventRequest("Barbecue", null, now.plusDays(7), now.plusDays(7).plusHours(3),
                null, null, null, false, "Kuba", null, null, null, null, null);

        MvcResult result = mvc.perform(json(post("/public/event"), request))
                .andExpect(status().isCreated())
                .andReturn();

        String slug = objectMapper.readTree(result.getResponse().getContentAsString()).get("slug").asText();
        JsonNode body = objectMapper.readTree(
                mvc.perform(get("/public/event/" + slug)).andReturn().getResponse().getContentAsString());

        assertThat(body.get("schedulingMode").asText()).isEqualTo(SchedulingMode.FIXED.name());
        assertThat(body.get("status").asText()).isEqualTo(EventStatus.CONFIRMED.name());
        assertThat(body.get("slots")).isEmpty();
    }

    private record Created(String slug, Cookie session) {
    }

    private Created createPollEvent(int minAttendees) throws Exception {
        CreateEventRequest request = new CreateEventRequest("Volleyball", null, null, null, null, null,
                null, false, "Kuba", null, SchedulingMode.POLL, minAttendees, deadline, fourDates);

        MvcResult result = mvc.perform(json(post("/public/event"), request))
                .andExpect(status().isCreated())
                .andReturn();

        return new Created(
                objectMapper.readTree(result.getResponse().getContentAsString()).get("slug").asText(),
                result.getResponse().getCookie(EventSessionService.COOKIE_NAME)
        );
    }

    private org.springframework.test.web.servlet.ResultActions join(Created event, String name, List<SlotVoteRequest> votes) throws Exception {
        return mvc.perform(json(post("/public/event/" + event.slug + "/session"),
                new OpenSessionRequest(name, null, null, votes)));
    }

    private Cookie joinAndKeepSession(Created event, String name, List<SlotVoteRequest> votes) throws Exception {
        return join(event, name, votes).andReturn().getResponse().getCookie(EventSessionService.COOKIE_NAME);
    }

    private JsonNode getEvent(Created event) throws Exception {
        return objectMapper.readTree(mvc.perform(get("/public/event/" + event.slug))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private UUID slotId(Created event, int index) throws Exception {
        List<JsonNode> slots = new java.util.ArrayList<>();
        getEvent(event).get("slots").forEach(slots::add);
        slots.sort(java.util.Comparator.comparing(slot -> slot.get("startDate").asText()));
        return UUID.fromString(slots.get(index).get("id").asText());
    }

    private static JsonNode slotById(JsonNode body, UUID slotId) {
        for (JsonNode slot : body.get("slots")) {
            if (slot.get("id").asText().equals(slotId.toString())) {
                return slot;
            }
        }
        throw new AssertionError("no slot " + slotId);
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, Object body) throws Exception {
        return builder.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
    }
}
