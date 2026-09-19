package com.app.prod.event.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.EventMember;
import org.jooq.sources.tables.records.EventMemberRecord;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import static org.jooq.sources.Tables.EVENT_MEMBER;

@Repository
public class EventMemberRepository extends BaseJooqRepository<EventMember, EventMemberRecord, UUID> {

    protected EventMemberRepository(DSLContext dsl) {
        super(dsl, EVENT_MEMBER, EVENT_MEMBER.ID);
    }
}
