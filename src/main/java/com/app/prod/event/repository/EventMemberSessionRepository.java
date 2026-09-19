package com.app.prod.event.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.EventMemberSession;
import org.jooq.sources.tables.records.EventMemberSessionRecord;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import static org.jooq.sources.Tables.EVENT_MEMBER_SESSION;

@Repository
public class EventMemberSessionRepository extends BaseJooqRepository<EventMemberSession, EventMemberSessionRecord, UUID> {

    protected EventMemberSessionRepository(DSLContext dsl) {
        super(dsl, EVENT_MEMBER_SESSION, EVENT_MEMBER_SESSION.ID);
    }
}
