package com.app.prod.event.repository;

import com.app.prod.event.enums.EventMemberStatus;
import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.EventMember;
import org.jooq.sources.tables.records.EventMemberRecord;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import static org.jooq.sources.Tables.EVENT_MEMBER;

@Repository
public class EventMemberRepository extends BaseJooqRepository<EventMember, EventMemberRecord, UUID> {

    protected EventMemberRepository(DSLContext dsl) {
        super(dsl, EVENT_MEMBER, EVENT_MEMBER.ID);
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
