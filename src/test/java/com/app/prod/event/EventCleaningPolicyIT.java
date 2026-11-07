package com.app.prod.event;

import com.app.prod.builders.AreaPersistenceFactory;
import com.app.prod.builders.BuildingPersistenceFactory;
import com.app.prod.builders.EventPersistenceFactory;
import com.app.prod.builders.PostPersistenceFactory;
import com.app.prod.builders.UserPersistanceFactory;
import com.app.prod.cleaning.DatabaseCleaningService;
import com.app.prod.cleaning.dtos.DatabaseCleaningResponse;
import com.app.prod.cleaning.dtos.EntityCleaningResult;
import com.app.prod.config.IntegrationTest;
import com.app.prod.config.MutableClock;
import com.app.prod.event.enums.EventStatus;
import com.app.prod.post.enums.PostType;
import org.jooq.DSLContext;
import org.jooq.sources.tables.records.EventMemberRecord;
import org.jooq.sources.tables.records.EventRecord;
import org.jooq.sources.tables.records.EventSlotRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.sources.Tables.EVENT;
import static org.jooq.sources.Tables.EVENT_MEMBER;
import static org.jooq.sources.Tables.EVENT_MEMBER_METADATA;
import static org.jooq.sources.Tables.EVENT_MEMBER_SESSION;
import static org.jooq.sources.Tables.EVENT_SLOT;
import static org.jooq.sources.Tables.EVENT_SLOT_VOTE;

@SpringBootTest
@Transactional
class EventCleaningPolicyIT extends IntegrationTest {

    @Autowired
    private EventPersistenceFactory events;
    @Autowired
    private PostPersistenceFactory posts;
    @Autowired
    private AreaPersistenceFactory areas;
    @Autowired
    private BuildingPersistenceFactory buildings;
    @Autowired
    private UserPersistanceFactory users;
    @Autowired
    private DatabaseCleaningService databaseCleaningService;
    @Autowired
    private DSLContext dsl;
    @Autowired
    private MutableClock clock;

    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now(clock);
    }

    @Test
    void shouldPurgeAFixedEventAMonthAfterItEnded() {
        EventRecord event = fixed(now.minusDays(40), now.minusDays(31));

        cleanExpiredEntities();

        assertThat(exists(event)).isFalse();
    }

    @Test
    void shouldKeepAFixedEventThatEndedLessThanAMonthAgo() {
        EventRecord event = fixed(now.minusDays(40), now.minusDays(29));

        cleanExpiredEntities();

        assertThat(exists(event)).isTrue();
    }

    @Test
    void shouldMeasureFromTheEndNotTheStart() {
        EventRecord longEvent = fixed(now.minusDays(60), now.minusDays(1));

        cleanExpiredEntities();

        assertThat(exists(longEvent)).as("started long ago, but ended yesterday").isTrue();
    }

    @Test
    void shouldFallBackToTheStartWhenTheEventHasNoEnd() {
        EventRecord expired = fixed(now.minusDays(31), null);
        EventRecord recent = fixed(now.minusDays(29), null);

        cleanExpiredEntities();

        assertThat(exists(expired)).isFalse();
        assertThat(exists(recent)).isTrue();
    }

    @Test
    void shouldFallBackToCreationWhenTheEventHasNoDateAtAll() {
        EventRecord expired = events.getNewEvent().fixed().startDateTime(null).createdAt(now.minusDays(31)).buildAndSave();
        EventRecord recent = events.getNewEvent().fixed().startDateTime(null).createdAt(now.minusDays(29)).buildAndSave();

        cleanExpiredEntities();

        assertThat(exists(expired)).isFalse();
        assertThat(exists(recent)).isTrue();
    }

    @Test
    void shouldPurgeAPollThatNeverFormedAMonthAfterItsVotingDeadline() {
        EventRecord expired = events.getNewEvent().poll(3, now.minusDays(31)).buildAndSave();
        EventRecord recent = events.getNewEvent().poll(3, now.minusDays(29)).buildAndSave();

        cleanExpiredEntities();

        assertThat(exists(expired)).isFalse();
        assertThat(exists(recent)).isTrue();
    }

    @Test
    void shouldMeasureAFormedGroupFromItsConfirmByDeadline() {
        EventRecord expired = formedGroup(now.minusDays(31));
        EventRecord recent = formedGroup(now.minusDays(29));

        cleanExpiredEntities();

        assertThat(exists(expired)).isFalse();
        assertThat(exists(recent)).isTrue();
    }

    @Test
    void shouldNeverPurgeAnEventPublishedInABuilding() {
        EventRecord event = fixed(now.minusDays(400), now.minusDays(399));
        UUID buildingId = buildings.getNewBuilding().withRandomValues()
                .areaId(areas.getNewArea().withRandomValues().buildAndSave().getId())
                .buildAndSave().getId();
        UUID authorId = users.getNewUser().withRandomValues().buildingId(buildingId).buildAndSave().getId();
        posts.getNewPost(PostType.EVENT).withRandomValues()
                .buildingId(buildingId).createdBy(authorId).eventId(event.getId())
                .buildAndSave();

        cleanExpiredEntities();

        assertThat(exists(event)).isTrue();
    }

    @Test
    void shouldTakeEverythingHangingOffTheEventWithIt() {
        EventRecord event = fixed(now.minusDays(40), now.minusDays(31));
        EventMemberRecord member = events.getNewMember().withRandomValues().eventId(event.getId()).buildAndSave();
        EventSlotRecord slot = events.getNewSlot().withRandomValues().eventId(event.getId()).buildAndSave();
        events.getNewVote().withRandomValues().slotId(slot.getId()).memberId(member.getId()).buildAndSave();
        dsl.insertInto(EVENT_MEMBER_SESSION)
                .set(EVENT_MEMBER_SESSION.MEMBER_ID, member.getId())
                .set(EVENT_MEMBER_SESSION.TOKEN_HASH, UUID.randomUUID().toString())
                .set(EVENT_MEMBER_SESSION.EXPIRES_AT, now.plusDays(1))
                .execute();
        dsl.insertInto(EVENT_MEMBER_METADATA)
                .set(EVENT_MEMBER_METADATA.MEMBER_ID, member.getId())
                .set(EVENT_MEMBER_METADATA.DEVICE_KEY, "device")
                .execute();

        cleanExpiredEntities();

        assertThat(dsl.fetchExists(EVENT_MEMBER, EVENT_MEMBER.EVENT_ID.eq(event.getId()))).isFalse();
        assertThat(dsl.fetchExists(EVENT_MEMBER_SESSION, EVENT_MEMBER_SESSION.MEMBER_ID.eq(member.getId()))).isFalse();
        assertThat(dsl.fetchExists(EVENT_MEMBER_METADATA, EVENT_MEMBER_METADATA.MEMBER_ID.eq(member.getId()))).isFalse();
        assertThat(dsl.fetchExists(EVENT_SLOT, EVENT_SLOT.EVENT_ID.eq(event.getId()))).isFalse();
        assertThat(dsl.fetchExists(EVENT_SLOT_VOTE, EVENT_SLOT_VOTE.SLOT_ID.eq(slot.getId()))).isFalse();
    }

    @Test
    void shouldOnlyCountOnADryRun() {
        EventRecord event = fixed(now.minusDays(40), now.minusDays(31));

        DatabaseCleaningResponse response = databaseCleaningService.cleanExpiredEntities(now, true);

        assertThat(response.dryRun()).isTrue();
        assertThat(eventResult(response).cleanedEntityCount()).isPositive();
        assertThat(exists(event)).isTrue();
    }

    @Test
    void shouldReportWhatItDeleted() {
        fixed(now.minusDays(40), now.minusDays(31));
        fixed(now.minusDays(40), now.minusDays(31));

        EntityCleaningResult result = eventResult(cleanExpiredEntities());

        assertThat(result.cleanedEntityCount()).isGreaterThanOrEqualTo(2);
        assertThat(result.failed()).isFalse();
    }

    private EventRecord fixed(LocalDateTime start, LocalDateTime end) {
        return events.getNewEvent().fixed().startDateTime(start).endDateTime(end).buildAndSave();
    }

    private EventRecord formedGroup(LocalDateTime confirmBy) {
        EventRecord event = events.getNewEvent().poll(3, confirmBy.minusDays(1)).buildAndSave();
        EventSlotRecord slot = events.getNewSlot().withRandomValues().eventId(event.getId()).startDateTime(confirmBy.plusDays(1)).buildAndSave();
        dsl.update(EVENT)
                .set(EVENT.STATUS, EventStatus.GROUP_FORMED.name())
                .set(EVENT.SELECTED_SLOT_ID, slot.getId())
                .set(EVENT.CONFIRM_BY, confirmBy)
                .where(EVENT.ID.eq(event.getId()))
                .execute();
        return event;
    }

    private DatabaseCleaningResponse cleanExpiredEntities() {
        return databaseCleaningService.cleanExpiredEntities(now, false);
    }

    private boolean exists(EventRecord event) {
        return dsl.fetchExists(EVENT, EVENT.ID.eq(event.getId()));
    }

    private static EntityCleaningResult eventResult(DatabaseCleaningResponse response) {
        return response.entityCleaningResults().stream()
                .filter(result -> result.entityName().equals("event"))
                .findFirst()
                .orElseThrow();
    }
}
