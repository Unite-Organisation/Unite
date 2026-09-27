package com.app.prod.event.repository;

import com.app.prod.event.dto.EventMemberRow;
import com.app.prod.event.enums.SlotPreference;
import com.app.prod.event.enums.EventMemberRole;
import com.app.prod.event.enums.EventMemberStatus;
import com.app.prod.utils.BaseJooqRepository;
import com.app.prod.utils.filters.EventMemberFilter;
import org.jooq.DSLContext;
import org.jooq.sources.tables.EventMember;
import org.jooq.sources.tables.records.EventMemberRecord;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.lower;
import static org.jooq.sources.Tables.EVENT_MEMBER;
import static org.jooq.sources.Tables.EVENT_SLOT_VOTE;

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

    public List<UUID> findGoingVotersForSlot(UUID eventId, UUID slotId) {
        return dslContext.select(EVENT_MEMBER.ID)
                .from(EVENT_MEMBER)
                .join(EVENT_SLOT_VOTE).on(EVENT_SLOT_VOTE.MEMBER_ID.eq(EVENT_MEMBER.ID))
                .where(EVENT_MEMBER.EVENT_ID.eq(eventId))
                .and(EVENT_SLOT_VOTE.SLOT_ID.eq(slotId))
                .and(EVENT_MEMBER.STATUS.eq(EventMemberStatus.GOING.name()))
                .orderBy(
                        field(EVENT_SLOT_VOTE.PREFERENCE.eq(SlotPreference.PREFERRED.name())).desc(),
                        EVENT_SLOT_VOTE.CREATED_AT,
                        EVENT_MEMBER.ID
                )
                .fetch(EVENT_MEMBER.ID);
    }

    public int moveStatus(UUID eventId, EventMemberStatus from, EventMemberStatus to, LocalDateTime now) {
        return dslContext.update(EVENT_MEMBER)
                .set(EVENT_MEMBER.STATUS, to.name())
                .set(EVENT_MEMBER.STATUS_CHANGED_AT, now)
                .where(EVENT_MEMBER.EVENT_ID.eq(eventId))
                .and(EVENT_MEMBER.STATUS.eq(from.name()))
                .execute();
    }

    public int setStatus(Collection<UUID> memberIds, EventMemberStatus status, LocalDateTime now) {
        if (memberIds.isEmpty()) {
            return 0;
        }
        return dslContext.update(EVENT_MEMBER)
                .set(EVENT_MEMBER.STATUS, status.name())
                .set(EVENT_MEMBER.STATUS_CHANGED_AT, now)
                .where(EVENT_MEMBER.ID.in(memberIds))
                .execute();
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
