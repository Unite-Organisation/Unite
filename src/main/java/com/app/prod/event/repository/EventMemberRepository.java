package com.app.prod.event.repository;

import com.app.prod.event.dto.EventMemberRow;
import com.app.prod.event.enums.EventMemberRole;
import com.app.prod.event.enums.EventMemberStatus;
import com.app.prod.utils.BaseJooqRepository;
import com.app.prod.utils.filters.EventMemberFilter;
import org.jooq.DSLContext;
import org.jooq.sources.tables.EventMember;
import org.jooq.sources.tables.records.EventMemberRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.impl.DSL.lower;
import static org.jooq.sources.Tables.EVENT_MEMBER;

@Repository
public class EventMemberRepository extends BaseJooqRepository<EventMember, EventMemberRecord, UUID> {

    private static final int MAX_MEMBERS = 1000;

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

    public List<EventMemberRow> findMembers(EventMemberFilter filter) {
        return dslContext.select(
                        EVENT_MEMBER.ID,
                        EVENT_MEMBER.DISPLAY_NAME,
                        EVENT_MEMBER.ROLE,
                        EVENT_MEMBER.STATUS
                )
                .from(EVENT_MEMBER)
                .where(filter.parseFilter())
                .orderBy(EVENT_MEMBER.DISPLAY_NAME, EVENT_MEMBER.ID)
                .limit(MAX_MEMBERS)
                .fetch(record -> new EventMemberRow(
                        record.get(EVENT_MEMBER.ID),
                        record.get(EVENT_MEMBER.DISPLAY_NAME),
                        EventMemberRole.valueOf(record.get(EVENT_MEMBER.ROLE)),
                        EventMemberStatus.valueOf(record.get(EVENT_MEMBER.STATUS))
                ));
    }
}
