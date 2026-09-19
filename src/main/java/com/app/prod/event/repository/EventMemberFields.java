package com.app.prod.event.repository;

import com.app.prod.event.enums.EventMemberStatus;
import com.app.prod.utils.filters.Related;
import org.jooq.Condition;
import org.jooq.Field;

import java.util.UUID;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.selectCount;
import static org.jooq.sources.Tables.EVENT_MEMBER;

public class EventMemberFields {

    public static Field<Integer> goingCount(Field<UUID> eventId) {
        return field(selectCount()
                .from(EVENT_MEMBER)
                .where(EVENT_MEMBER.EVENT_ID.eq(eventId))
                .and(EVENT_MEMBER.STATUS.eq(EventMemberStatus.GOING.name())));
    }

    public static Condition goingBy(Field<UUID> eventId, UUID userId) {
        return Related.existsIn(EVENT_MEMBER,
                EVENT_MEMBER.EVENT_ID.eq(eventId),
                EVENT_MEMBER.USER_ID.eq(userId),
                EVENT_MEMBER.STATUS.eq(EventMemberStatus.GOING.name()));
    }
}
