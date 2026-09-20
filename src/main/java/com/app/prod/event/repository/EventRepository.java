package com.app.prod.event.repository;

import com.app.prod.event.dto.EventResponse;
import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.sources.tables.Event;
import org.jooq.sources.tables.records.EventRecord;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import static org.jooq.sources.Tables.EVENT;

@Repository
public class EventRepository extends BaseJooqRepository<Event, EventRecord, UUID> {

    protected EventRepository(DSLContext dsl) {
        super(dsl, EVENT, EVENT.ID);
    }

    public Optional<EventRecord> findForUpdate(UUID eventId) {
        return dslContext.selectFrom(EVENT)
                .where(EVENT.ID.eq(eventId))
                .forUpdate()
                .fetchOptional();
    }

    public Optional<EventRecord> findBySlugForUpdate(String slug) {
        return dslContext.selectFrom(EVENT)
                .where(EVENT.PUBLIC_SLUG.eq(slug))
                .forUpdate()
                .fetchOptional();
    }

    public Optional<UUID> findIdBySlug(String slug) {
        return dslContext.select(EVENT.ID)
                .from(EVENT)
                .where(EVENT.PUBLIC_SLUG.eq(slug))
                .fetchOptional(EVENT.ID);
    }

    public Optional<EventResponse> findBySlug(String slug) {
        Field<Integer> goingCount = EventMemberFields.goingCount(EVENT.ID).as("going_count");

        return dslContext.select(
                        EVENT.PUBLIC_SLUG,
                        EVENT.NAME,
                        EVENT.DESCRIPTION,
                        EVENT.START_DATE_TIME,
                        EVENT.END_DATE_TIME,
                        EVENT.LOCATION_NAME,
                        EVENT.ONLINE_URL,
                        EVENT.MAX_ATTENDEES,
                        EVENT.WAITLIST_ENABLED,
                        goingCount,
                        EVENT.CREATED_AT
                )
                .from(EVENT)
                .where(EVENT.PUBLIC_SLUG.eq(slug))
                .fetchOptional(record -> new EventResponse(
                        record.get(EVENT.PUBLIC_SLUG),
                        record.get(EVENT.NAME),
                        record.get(EVENT.DESCRIPTION),
                        record.get(EVENT.START_DATE_TIME),
                        record.get(EVENT.END_DATE_TIME),
                        record.get(EVENT.LOCATION_NAME),
                        record.get(EVENT.ONLINE_URL),
                        record.get(EVENT.MAX_ATTENDEES),
                        record.get(EVENT.WAITLIST_ENABLED),
                        record.get(goingCount),
                        record.get(EVENT.CREATED_AT),
                        null
                ));
    }
}
