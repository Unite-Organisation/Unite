package com.app.prod.utils.filters;

import com.app.prod.event.enums.EventMemberRole;
import com.app.prod.event.enums.EventMemberStatus;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.jooq.Condition;

import java.util.List;
import java.util.UUID;

import static org.jooq.sources.Tables.EVENT_MEMBER;

@Getter
@SuperBuilder
public class EventMemberFilter extends PredicateFilter {
    Filter<UUID> eventId;
    Filter<EventMemberStatus> status;
    Filter<EventMemberRole> role;

    @Override
    public List<Condition> combineConditions() {
        return Criteria.of(
                Criteria.required(EVENT_MEMBER.EVENT_ID, eventId),
                Criteria.matchEnum(EVENT_MEMBER.STATUS, status),
                Criteria.matchEnum(EVENT_MEMBER.ROLE, role),
                Criteria.search(search(), EVENT_MEMBER.DISPLAY_NAME)
        );
    }
}
