package com.app.prod.event.repository;

import com.app.prod.event.dto.SessionMember;
import com.app.prod.event.enums.EventMemberRole;
import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.EventMemberSession;
import org.jooq.sources.tables.records.EventMemberSessionRecord;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.sources.Tables.EVENT;
import static org.jooq.sources.Tables.EVENT_MEMBER;
import static org.jooq.sources.Tables.EVENT_MEMBER_SESSION;

@Repository
public class EventMemberSessionRepository extends BaseJooqRepository<EventMemberSession, EventMemberSessionRecord, UUID> {

    protected EventMemberSessionRepository(DSLContext dsl) {
        super(dsl, EVENT_MEMBER_SESSION, EVENT_MEMBER_SESSION.ID);
    }

    public Optional<SessionMember> findActiveMember(String tokenHash, String slug, LocalDateTime now) {
        return dslContext.select(
                        EVENT_MEMBER.EVENT_ID,
                        EVENT_MEMBER.ID,
                        EVENT_MEMBER.ROLE,
                        EVENT_MEMBER.USER_ID
                )
                .from(EVENT_MEMBER_SESSION)
                .join(EVENT_MEMBER).on(EVENT_MEMBER.ID.eq(EVENT_MEMBER_SESSION.MEMBER_ID))
                .join(EVENT).on(EVENT.ID.eq(EVENT_MEMBER.EVENT_ID))
                .where(EVENT_MEMBER_SESSION.TOKEN_HASH.eq(tokenHash))
                .and(EVENT_MEMBER_SESSION.EXPIRES_AT.gt(now))
                .and(EVENT.PUBLIC_SLUG.eq(slug))
                .fetchOptional(record -> new SessionMember(
                        record.get(EVENT_MEMBER.EVENT_ID),
                        record.get(EVENT_MEMBER.ID),
                        EventMemberRole.valueOf(record.get(EVENT_MEMBER.ROLE)),
                        record.get(EVENT_MEMBER.USER_ID)
                ));
    }
}
