package com.app.prod.event;

import com.app.prod.builders.AreaPersistenceFactory;
import com.app.prod.builders.BuildingPersistenceFactory;
import com.app.prod.builders.UserPersistanceFactory;
import com.app.prod.config.IntegrationTest;
import com.app.prod.config.MutableClock;
import com.app.prod.event.dto.SlotRequest;
import com.app.prod.event.enums.EventMemberStatus;
import com.app.prod.event.enums.EventStatus;
import com.app.prod.event.enums.SchedulingMode;
import com.app.prod.event.EventApi.Event;
import com.app.prod.event.EventApi.Person;
import com.app.prod.user.enums.UserRole;
import com.app.prod.utils.JwtTestHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.jooq.sources.tables.records.AppUserRecord;
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

import static com.app.prod.event.EventApi.onlyIfNeeded;
import static com.app.prod.event.EventApi.suits;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.sources.Tables.EVENT;
import static org.jooq.sources.Tables.SCHEDULED_JOB;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Whole stories rather than single calls: every one of these runs a group of people through the api
 * the way they would actually use it, and checks what each of them ends up with.
 * <p>
 * Not {@code @Transactional} - a drain claims its work in a transaction of its own, and these
 * journeys depend on it.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PublicEventJourneyIT extends IntegrationTest {

    private static final Duration WALK_OUT_WINDOW = Duration.ofHours(1);

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
    private AreaPersistenceFactory areas;
    @Autowired
    private BuildingPersistenceFactory buildings;
    @Autowired
    private JwtTestHelper jwt;

    private EventApi api;
    private LocalDateTime now;
    private LocalDateTime deadline;
    private List<SlotRequest> fourDates;

    @BeforeEach
    void setUp() throws Exception {
        dslContext.deleteFrom(EVENT).execute();
        dslContext.deleteFrom(SCHEDULED_JOB).execute();

        clock.setInstant(Instant.now().truncatedTo(ChronoUnit.MICROS));
        api = new EventApi(mvc, objectMapper);

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
    void sixPeopleTurnAnEmptyEventIntoAConfirmedDate() throws Exception {
        Event event = api.createEventChoosingItsDate("Kuba", 5, deadline, fourDates);
        Dates dates = dates(event);

        assertThat(api.statusOf(event)).isEqualTo(EventStatus.COLLECTING_VOTES.name());
        assertThat(api.date(event, dates.monday).get("preferredCount").asInt())
                .as("the host proposed these dates, so all of them suit them")
                .isEqualTo(1);
        assertThat(api.date(event, dates.monday).get("chance").asInt()).isEqualTo(20);

        Person anna = api.join(event, "Anna", suits(dates.monday), onlyIfNeeded(dates.tuesday));
        assertThat(api.date(event, dates.monday).get("chance").asInt()).isEqualTo(40);
        assertThat(api.date(event, dates.tuesday).get("chance").asInt())
                .as("a reluctant yes is worth half a sure one on the tile")
                .isEqualTo(30);

        api.join(event, "Bartek", suits(dates.monday));
        Person celina = api.join(event, "Celina", suits(dates.tuesday), onlyIfNeeded(dates.wednesday));
        api.join(event, "Dawid", suits(dates.monday));

        assertThat(api.statusOf(event)).isEqualTo(EventStatus.COLLECTING_VOTES.name());
        assertThat(topDate(event)).as("the fullest date is shown first").isEqualTo(dates.monday);
        assertThat(api.date(event, dates.monday).get("chance").asInt()).isEqualTo(80);

        api.join(event, "Ewa", suits(dates.monday));

        JsonNode formed = api.event(event);
        assertThat(formed.get("status").asText()).isEqualTo(EventStatus.GROUP_FORMED.name());
        assertThat(formed.get("selectedSlotId").asText()).isEqualTo(dates.monday.toString());
        assertThat(formed.get("slots")).allSatisfy(date ->
                assertThat(date.get("open").asBoolean())
                        .as("once the date is picked the others are closed")
                        .isFalse());

        api.tryChangeChoice(event, celina, suits(dates.tuesday)).andExpect(status().isBadRequest());
        api.tryJoin(event, "Spozniony", List.of(suits(dates.monday))).andExpect(status().isBadRequest());

        clock.advance(WALK_OUT_WINDOW);
        api.runDueJobs();

        JsonNode confirmed = api.event(event);
        assertThat(confirmed.get("status").asText()).isEqualTo(EventStatus.CONFIRMED.name());
        assertThat(confirmed.get("startDate").asText())
                .as("a settled event is an ordinary dated event")
                .startsWith(fourDates.getFirst().startDate().toString().substring(0, 16));
        assertThat(confirmed.get("goingCount").asInt()).isEqualTo(5);

        assertThat(api.namesWithStatus(event, EventMemberStatus.GOING))
                .containsExactlyInAnyOrder("Kuba", "Anna", "Bartek", "Dawid", "Ewa");
        assertThat(api.namesWithStatus(event, EventMemberStatus.NOT_AVAILABLE))
                .as("Celina never said no - the date went the other way")
                .containsExactly("Celina");

        assertThat(api.eventSeenBy(event, anna).get("me").get("status").asText())
                .isEqualTo(EventMemberStatus.GOING.name());
    }

    @Test
    void aGroupFallsApartWhenSomeoneLeavesAndComesTogetherAgainWithSomeoneElse() throws Exception {
        Event event = api.createEventChoosingItsDate("Kuba", 3, deadline, fourDates);
        Dates dates = dates(event);

        Person anna = api.join(event, "Anna", suits(dates.monday));
        api.join(event, "Bartek", suits(dates.monday));
        assertThat(api.statusOf(event)).isEqualTo(EventStatus.GROUP_FORMED.name());

        api.walkOut(event, anna);

        JsonNode reopened = api.event(event);
        assertThat(reopened.get("status").asText())
                .as("having enough people is the condition for the group, not a gate passed once")
                .isEqualTo(EventStatus.COLLECTING_VOTES.name());
        assertThat(reopened.get("selectedSlotId").isNull()).isTrue();
        assertThat(reopened.get("slots")).allSatisfy(date ->
                assertThat(date.get("open").asBoolean()).isTrue());

        api.join(event, "Celina", suits(dates.monday));
        assertThat(api.statusOf(event)).isEqualTo(EventStatus.GROUP_FORMED.name());

        api.startNow(event, event.host());

        assertThat(api.statusOf(event)).isEqualTo(EventStatus.CONFIRMED.name());
        assertThat(api.namesWithStatus(event, EventMemberStatus.GOING))
                .containsExactlyInAnyOrder("Kuba", "Bartek", "Celina");
        assertThat(api.namesWithStatus(event, EventMemberStatus.NOT_GOING))
                .as("Anna walked out - that is her decision, not the system's")
                .containsExactly("Anna");
    }

    @Test
    void theDeadlinePassesShortOfPeopleAndTheHostFillsTheLastDateLeft() throws Exception {
        Event event = api.createEventChoosingItsDate("Kuba", 5, deadline, fourDates);
        Dates dates = dates(event);

        api.join(event, "Anna", suits(dates.monday));
        Person bartek = api.join(event, "Bartek", suits(dates.tuesday));

        clock.setInstant(deadline.atZone(clock.getZone()).toInstant());
        api.runDueJobs();

        JsonNode narrowed = api.event(event);
        assertThat(narrowed.get("status").asText()).isEqualTo(EventStatus.ONE_SLOT_LEFT.name());
        assertThat(narrowed.get("selectedSlotId").asText()).isEqualTo(dates.monday.toString());
        assertThat(api.date(event, dates.tuesday).get("open").asBoolean())
                .as("the dropped dates are gone for good")
                .isFalse();

        api.tryJoin(event, "Celina", List.of(suits(dates.tuesday))).andExpect(status().isBadRequest());
        api.tryChangeChoice(event, bartek, suits(dates.tuesday)).andExpect(status().isBadRequest());

        api.join(event, "Celina", suits(dates.monday));
        api.join(event, "Dawid", suits(dates.monday));
        assertThat(api.statusOf(event)).isEqualTo(EventStatus.ONE_SLOT_LEFT.name());

        api.join(event, "Ewa", suits(dates.monday));
        assertThat(api.statusOf(event))
                .as("the last date can still carry the event")
                .isEqualTo(EventStatus.GROUP_FORMED.name());

        api.startNow(event, event.host());

        assertThat(api.namesWithStatus(event, EventMemberStatus.GOING))
                .containsExactlyInAnyOrder("Kuba", "Anna", "Celina", "Dawid", "Ewa");
        assertThat(api.namesWithStatus(event, EventMemberStatus.NOT_AVAILABLE))
                .as("Bartek only ever offered the date that was dropped")
                .containsExactly("Bartek");
    }

    @Test
    void aDateEveryoneWantsBeatsOneTheSameNumberOfPeopleWereTalkedInto() throws Exception {
        Event event = api.createEventChoosingItsDate("Kuba", 6, deadline, fourDates);
        Dates dates = dates(event);

        api.join(event, "Anna", suits(dates.monday));
        api.join(event, "Bartek", suits(dates.monday));
        api.join(event, "Celina", suits(dates.monday));
        api.join(event, "Dawid", suits(dates.monday));

        api.join(event, "Ewa", suits(dates.tuesday));
        api.join(event, "Filip", suits(dates.tuesday));
        api.join(event, "Grzegorz", onlyIfNeeded(dates.tuesday));
        api.join(event, "Hanna", onlyIfNeeded(dates.tuesday));

        assertThat(api.statusOf(event))
                .as("five on each date, six needed")
                .isEqualTo(EventStatus.COLLECTING_VOTES.name());

        api.join(event, "Iwona", suits(dates.monday), onlyIfNeeded(dates.tuesday));

        JsonNode formed = api.event(event);
        assertThat(formed.get("status").asText()).isEqualTo(EventStatus.GROUP_FORMED.name());
        assertThat(formed.get("selectedSlotId").asText())
                .as("both dates now hold six people; monday holds six who actually want it")
                .isEqualTo(dates.monday.toString());

        JsonNode tuesday = api.date(event, dates.tuesday);
        assertThat(tuesday.get("preferredCount").asInt()).isEqualTo(3);
        assertThat(tuesday.get("ifNeededCount").asInt()).isEqualTo(3);
    }

    @Test
    void seatsRunOutOnceTheDateIsSettledAndTheWaitlistTakesOver() throws Exception {
        Event event = api.createEventChoosingItsDate("Kuba", 2, 3, true, deadline, fourDates);
        Dates dates = dates(event);

        Person anna = api.join(event, "Anna", suits(dates.monday));
        assertThat(api.statusOf(event)).isEqualTo(EventStatus.GROUP_FORMED.name());

        api.startNow(event, event.host());
        assertThat(api.statusOf(event)).isEqualTo(EventStatus.CONFIRMED.name());

        api.signUp(event, "Bartek");
        api.signUp(event, "Celina");

        assertThat(api.namesWithStatus(event, EventMemberStatus.GOING))
                .as("a settled event takes sign-ups like any other, up to its limit")
                .containsExactlyInAnyOrder("Kuba", "Anna", "Bartek");
        assertThat(api.namesWithStatus(event, EventMemberStatus.WAITLIST)).containsExactly("Celina");

        api.walkOut(event, anna);

        assertThat(api.namesWithStatus(event, EventMemberStatus.GOING))
                .as("a freed seat goes to whoever was waiting")
                .containsExactlyInAnyOrder("Kuba", "Bartek", "Celina");
        assertThat(api.namesWithStatus(event, EventMemberStatus.NOT_GOING)).containsExactly("Anna");
    }

    @Test
    void peopleChangeTheirMindsAndTheLeadingDateChangesWithThem() throws Exception {
        Event event = api.createEventChoosingItsDate("Kuba", 5, deadline, fourDates);
        Dates dates = dates(event);

        Person anna = api.join(event, "Anna", suits(dates.monday));
        api.join(event, "Bartek", suits(dates.monday));
        api.join(event, "Celina", suits(dates.tuesday));
        api.join(event, "Dawid", suits(dates.tuesday));

        assertThat(topDate(event))
                .as("level on people, so the earlier date leads")
                .isEqualTo(dates.monday);

        api.changeChoice(event, anna, suits(dates.tuesday));

        assertThat(topDate(event)).isEqualTo(dates.tuesday);
        assertThat(api.date(event, dates.monday).get("preferredCount").asInt()).isEqualTo(2);
        assertThat(api.date(event, dates.tuesday).get("preferredCount").asInt()).isEqualTo(4);

        api.join(event, "Ewa", suits(dates.tuesday));
        api.startNow(event, event.host());

        assertThat(api.namesWithStatus(event, EventMemberStatus.GOING))
                .as("Anna moved across in time and kept her seat")
                .containsExactlyInAnyOrder("Kuba", "Anna", "Celina", "Dawid", "Ewa");
        assertThat(api.namesWithStatus(event, EventMemberStatus.NOT_AVAILABLE)).containsExactly("Bartek");
    }

    @Test
    void theGroupReformsOnAnotherDateThatAlreadyHadEnoughPeople() throws Exception {
        Event event = api.createEventChoosingItsDate("Kuba", 3, deadline, fourDates);
        Dates dates = dates(event);

        Person anna = api.join(event, "Anna", suits(dates.monday));
        api.join(event, "Bartek", suits(dates.tuesday));
        api.join(event, "Celina", suits(dates.monday), suits(dates.tuesday));

        JsonNode formed = api.event(event);
        assertThat(formed.get("status").asText()).isEqualTo(EventStatus.GROUP_FORMED.name());
        assertThat(formed.get("selectedSlotId").asText())
                .as("both dates filled at once, the earlier one wins the tie")
                .isEqualTo(dates.monday.toString());

        api.walkOut(event, anna);

        JsonNode after = api.event(event);
        assertThat(after.get("status").asText())
                .as("tuesday still holds three people, so the group does not have to wait for anyone")
                .isEqualTo(EventStatus.GROUP_FORMED.name());
        assertThat(after.get("selectedSlotId").asText()).isEqualTo(dates.tuesday.toString());
    }

    @Test
    void aUniteUserAndGuestsFillTheSameEvent() throws Exception {
        AppUserRecord user = uniteUser();
        String token = jwt.generateTokenForUser(user, UserRole.RESIDENT);

        Event event = api.createEventChoosingItsDate("Kuba", 3, deadline, fourDates);
        Dates dates = dates(event);

        api.join(event, "Anna", suits(dates.monday));
        api.joinAsUniteUser(event, token, suits(dates.monday));

        assertThat(api.statusOf(event)).isEqualTo(EventStatus.GROUP_FORMED.name());

        api.startNow(event, event.host());

        String uniteName = (user.getFirstName() + " " + user.getLastName()).trim();
        assertThat(api.namesWithStatus(event, EventMemberStatus.GOING))
                .as("an account holder and people off a link sit in the same event")
                .containsExactlyInAnyOrder("Kuba", "Anna", uniteName);
    }

    @Test
    void aGuestLosesTheirPhoneAndComesBackWithTheirCode() throws Exception {
        Event event = api.createEventChoosingItsDate("Kuba", 4, deadline, fourDates);
        Dates dates = dates(event);

        Person anna = api.join(event, "Anna", suits(dates.monday));
        assertThat(anna.returnCode()).as("a guest gets a code to come back with").isNotNull();

        api.tryResumeWithCode(event, "Anna", "0000".equals(anna.returnCode()) ? "1111" : "0000")
                .andExpect(status().isUnauthorized());

        Person annaAgain = api.resumeWithCode(event, "Anna", anna.returnCode());

        assertThat(api.date(event, dates.monday).get("preferredCount").asInt())
                .as("coming back changes nothing about what she already said")
                .isEqualTo(2);
        assertThat(api.eventSeenBy(event, annaAgain).get("myVotes")).hasSize(1);

        api.changeChoice(event, annaAgain, suits(dates.monday), suits(dates.tuesday));
        assertThat(api.date(event, dates.tuesday).get("preferredCount").asInt()).isEqualTo(2);
    }

    @Test
    void anEventThatAlreadyHasADateStillBehavesTheWayItAlwaysDid() throws Exception {
        Event event = api.createDatedEvent("Kuba", now.plusDays(7), 2, true);

        JsonNode body = api.event(event);
        assertThat(body.get("schedulingMode").asText()).isEqualTo(SchedulingMode.FIXED.name());
        assertThat(body.get("status").asText()).isEqualTo(EventStatus.CONFIRMED.name());
        assertThat(body.get("slots")).isEmpty();

        Person anna = api.signUp(event, "Anna");
        api.signUp(event, "Bartek");

        assertThat(api.namesWithStatus(event, EventMemberStatus.GOING)).containsExactlyInAnyOrder("Kuba", "Anna");
        assertThat(api.namesWithStatus(event, EventMemberStatus.WAITLIST)).containsExactly("Bartek");

        api.walkOut(event, anna);

        assertThat(api.namesWithStatus(event, EventMemberStatus.GOING)).containsExactlyInAnyOrder("Kuba", "Bartek");
    }

    @Test
    void onlyTheHostCanStartTheEventEarly() throws Exception {
        Event event = api.createEventChoosingItsDate("Kuba", 2, deadline, fourDates);
        Dates dates = dates(event);

        Person anna = api.join(event, "Anna", suits(dates.monday));
        assertThat(api.statusOf(event)).isEqualTo(EventStatus.GROUP_FORMED.name());

        api.tryStartNow(event, anna).andExpect(status().isForbidden());
        assertThat(api.statusOf(event)).isEqualTo(EventStatus.GROUP_FORMED.name());

        api.startNow(event, event.host());
        assertThat(api.statusOf(event)).isEqualTo(EventStatus.CONFIRMED.name());
    }

    private record Dates(UUID monday, UUID tuesday, UUID wednesday, UUID thursday) {
    }

    private Dates dates(Event event) {
        List<UUID> ids = api.datesInOrder(event);
        return new Dates(ids.get(0), ids.get(1), ids.get(2), ids.get(3));
    }

    private UUID topDate(Event event) {
        return UUID.fromString(api.event(event).get("slots").get(0).get("id").asText());
    }

    private AppUserRecord uniteUser() {
        var area = areas.getNewArea().withRandomValues().buildAndSave();
        var building = buildings.getNewBuilding().withRandomValues().areaId(area.getId()).buildAndSave();
        return users.getNewUser().withRandomValues().buildingId(building.getId()).buildAndSave();
    }
}
