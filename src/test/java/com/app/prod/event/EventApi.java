package com.app.prod.event;

import com.app.prod.event.dto.AttendanceRequest;
import com.app.prod.event.dto.CreateEventRequest;
import com.app.prod.event.dto.OpenSessionRequest;
import com.app.prod.event.dto.SlotRequest;
import com.app.prod.event.dto.SlotVoteRequest;
import com.app.prod.event.dto.VotesRequest;
import com.app.prod.event.enums.EventMemberStatus;
import com.app.prod.event.enums.SchedulingMode;
import com.app.prod.event.enums.SlotPreference;
import com.app.prod.event.service.EventSessionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The public event api as a person would use it, so a test can be written as a story rather than as
 * a pile of request builders. Everything goes through the controller - there is no back door into
 * the services here on purpose.
 */
class EventApi {

    private static final String DRAIN_TOKEN = "test-drain-token";

    private final MockMvc mvc;
    private final ObjectMapper objectMapper;

    EventApi(MockMvc mvc, ObjectMapper objectMapper) {
        this.mvc = mvc;
        this.objectMapper = objectMapper;
    }

    /** Whoever is holding a session - the host or any member. */
    record Person(String name, Cookie session, String returnCode) {
    }

    record Event(String slug, Person host) {
    }

    // ---------------------------------------------------------------- creating

    Event createEventChoosingItsDate(String hostName, int minAttendees, LocalDateTime deadline, List<SlotRequest> dates) {
        return createEventChoosingItsDate(hostName, minAttendees, null, false, deadline, dates);
    }

    Event createEventChoosingItsDate(String hostName, int minAttendees, Integer maxAttendees, boolean waitlist,
                                     LocalDateTime deadline, List<SlotRequest> dates) {
        return create(new CreateEventRequest("Volleyball", null, null, null, null, null,
                maxAttendees, waitlist, hostName, null, SchedulingMode.POLL, minAttendees, deadline, dates));
    }

    Event createDatedEvent(String hostName, LocalDateTime start, Integer maxAttendees, boolean waitlist) {
        return create(new CreateEventRequest("Barbecue", null, start, null, null, null,
                maxAttendees, waitlist, hostName, null, null, null, null, null));
    }

    private Event create(CreateEventRequest request) {
        MvcResult result = unchecked(() -> perform(post("/public/event"), request, null, null)
                .andExpect(status().isCreated())
                .andReturn());

        JsonNode body = read(result);
        return new Event(
                body.get("slug").asText(),
                new Person(request.displayName(), cookieOf(result), text(body, "returnCode"))
        );
    }

    // ---------------------------------------------------------------- joining

    Person join(Event event, String name, SlotVoteRequest... choice) {
        MvcResult result = unchecked(() -> tryJoin(event, name, List.of(choice))
                .andExpect(status().isCreated())
                .andReturn());
        return new Person(name, cookieOf(result), text(read(result), "returnCode"));
    }

    ResultActions tryJoin(Event event, String name, List<SlotVoteRequest> choice) {
        return perform(post(path(event, "/session")), new OpenSessionRequest(name, null, null, choice), null, null);
    }

    /** Signing up for an event that already has a date - no choosing involved. */
    Person signUp(Event event, String name) {
        MvcResult result = unchecked(() -> perform(path(event, "/session"), new OpenSessionRequest(name, null, null, null))
                .andExpect(status().isCreated())
                .andReturn());
        Person person = new Person(name, cookieOf(result), text(read(result), "returnCode"));
        say(event, person, EventMemberStatus.GOING);
        return person;
    }

    Person joinAsUniteUser(Event event, String jwt, SlotVoteRequest... choice) {
        MvcResult result = unchecked(() -> perform(post(path(event, "/session")),
                new OpenSessionRequest(null, null, null, List.of(choice)), null, jwt)
                .andExpect(status().isCreated())
                .andReturn());
        return new Person("unite", cookieOf(result), null);
    }

    /** Coming back on a different device, with only the name and the code to prove who you are. */
    Person resumeWithCode(Event event, String name, String returnCode) {
        MvcResult result = unchecked(() -> tryResumeWithCode(event, name, returnCode)
                .andExpect(status().isOk())
                .andReturn());
        return new Person(name, cookieOf(result), returnCode);
    }

    ResultActions tryResumeWithCode(Event event, String name, String returnCode) {
        return perform(post(path(event, "/session")), new OpenSessionRequest(name, returnCode, null, null), null, null);
    }

    // ---------------------------------------------------------------- acting

    void changeChoice(Event event, Person person, SlotVoteRequest... choice) {
        unchecked(() -> tryChangeChoice(event, person, choice).andExpect(status().isOk()));
    }

    ResultActions tryChangeChoice(Event event, Person person, SlotVoteRequest... choice) {
        return perform(put(path(event, "/votes")), new VotesRequest(List.of(choice)), person.session(), null);
    }

    void walkOut(Event event, Person person) {
        say(event, person, EventMemberStatus.NOT_GOING);
    }

    void comeBack(Event event, Person person) {
        say(event, person, EventMemberStatus.GOING);
    }

    void say(Event event, Person person, EventMemberStatus status) {
        unchecked(() -> perform(put(path(event, "/attendance")), new AttendanceRequest(status), person.session(), null)
                .andExpect(status().isOk()));
    }

    /** Signs out of the event without giving up membership in it. */
    void signOut(Event event, Person person) {
        unchecked(() -> perform(delete(path(event, "/session")), null, person.session(), null)
                .andExpect(status().isNoContent()));
    }

    void startNow(Event event, Person person) {
        unchecked(() -> tryStartNow(event, person).andExpect(status().isOk()));
    }

    ResultActions tryStartNow(Event event, Person person) {
        return perform(post(path(event, "/confirm")).cookie(person.session()), null, null, null);
    }

    /** What Cloud Scheduler does on a cron - the only way a deadline passes with nobody watching. */
    void runDueJobs() {
        unchecked(() -> mvc.perform(post("/internal/jobs/drain").header("X-Job-Token", DRAIN_TOKEN))
                .andExpect(status().isOk()));
    }

    // ---------------------------------------------------------------- reading

    JsonNode event(Event event) {
        return unchecked(() -> read(mvc.perform(get("/public/event/" + event.slug()))
                .andExpect(status().isOk())
                .andReturn()));
    }

    JsonNode eventSeenBy(Event event, Person person) {
        return unchecked(() -> read(mvc.perform(get("/public/event/" + event.slug()).cookie(person.session()))
                .andExpect(status().isOk())
                .andReturn()));
    }

    String statusOf(Event event) {
        return event(event).get("status").asText();
    }

    /** The proposed dates in the order the host wrote them down, not the order they are shown in. */
    List<UUID> datesInOrder(Event event) {
        List<JsonNode> slots = new ArrayList<>();
        event(event).get("slots").forEach(slots::add);
        slots.sort(Comparator.comparing(slot -> slot.get("startDate").asText()));
        return slots.stream().map(slot -> UUID.fromString(slot.get("id").asText())).toList();
    }

    JsonNode date(Event event, UUID slotId) {
        for (JsonNode slot : event(event).get("slots")) {
            if (slot.get("id").asText().equals(slotId.toString())) {
                return slot;
            }
        }
        throw new AssertionError("no date " + slotId + " on event " + event.slug());
    }

    List<String> namesWithStatus(Event event, EventMemberStatus status) {
        try {
            JsonNode members = read(mvc.perform(get("/public/event/" + event.slug() + "/members")
                            .param("status", status.name()))
                    .andExpect(status().isOk())
                    .andReturn());

            List<String> names = new ArrayList<>();
            members.forEach(member -> names.add(member.get("displayName").asText()));
            return names;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static SlotVoteRequest suits(UUID slotId) {
        return new SlotVoteRequest(slotId, SlotPreference.PREFERRED);
    }

    static SlotVoteRequest onlyIfNeeded(UUID slotId) {
        return new SlotVoteRequest(slotId, SlotPreference.IF_NEEDED);
    }

    // ---------------------------------------------------------------- plumbing

    private static <T> T unchecked(Callable<T> call) {
        try {
            return call.call();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private ResultActions perform(String path, Object body) {
        return perform(post(path), body, null, null);
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder, Object body, Cookie session, String jwt) {
        try {
            if (body != null) {
                builder.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
            }
            if (session != null) {
                builder.cookie(session);
            }
            if (jwt != null) {
                builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
            }
            return mvc.perform(builder);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String path(Event event, String suffix) {
        return "/public/event/" + event.slug() + suffix;
    }

    private JsonNode read(MvcResult result) {
        try {
            return objectMapper.readTree(result.getResponse().getContentAsString());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Cookie cookieOf(MvcResult result) {
        return result.getResponse().getCookie(EventSessionService.COOKIE_NAME);
    }

    private static String text(JsonNode body, String field) {
        JsonNode value = body.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
