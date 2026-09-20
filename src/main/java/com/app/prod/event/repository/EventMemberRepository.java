package com.app.prod.event.repository;

import com.app.prod.event.enums.EventMemberStatus;
import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.EventMember;
import org.jooq.sources.tables.records.EventMemberRecord;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import static org.jooq.impl.DSL.lower;
import static org.jooq.sources.Tables.EVENT_MEMBER;

@Repository
public class EventMemberRepository extends BaseJooqRepository<EventMember, EventMemberRecord, UUID> {

    protected EventMemberRepository(DSLContext dsl) {
        super(dsl, EVENT_MEMBER, EVENT_MEMBER.ID);
    }

    public Optional<EventMemberRecord> findUniteMember(UUID eventId, UUID userId) {
        return dslContext.selectFrom(EVENT_MEMBER)
                .where(EVENT_MEMBER.EVENT_ID.eq(eventId))
                .and(EVENT_MEMBER.USER_ID.eq(userId))
                .fetchOptional();
    }

    public Optional<EventMemberRecord> findGuestByNameForUpdate(UUID eventId, String displayName) {
        return dslContext.selectFrom(EVENT_MEMBER)
                .where(EVENT_MEMBER.EVENT_ID.eq(eventId))
                .and(EVENT_MEMBER.USER_ID.isNull())
                .and(lower(EVENT_MEMBER.DISPLAY_NAME).eq(lower(displayName)))
                .forUpdate()
                .fetchOptional();
    }

    public Optional<EventMemberRecord> findFirstWaitlisted(UUID eventId) {
        return dslContext.selectFrom(EVENT_MEMBER)
                .where(EVENT_MEMBER.EVENT_ID.eq(eventId))
                .and(EVENT_MEMBER.STATUS.eq(EventMemberStatus.WAITLIST.name()))
                .orderBy(EVENT_MEMBER.STATUS_CHANGED_AT, EVENT_MEMBER.ID)
                .limit(1)
                .fetchOptional();
    }

    public int countWithStatus(UUID eventId, EventMemberStatus status) {
        return Optional.ofNullable(
                dslContext.selectCount()
                        .from(EVENT_MEMBER)
                        .where(EVENT_MEMBER.EVENT_ID.eq(eventId))
                        .and(EVENT_MEMBER.STATUS.eq(status.name()))
                        .fetchOneInto(Integer.class)
        ).orElse(0);
    }
}
