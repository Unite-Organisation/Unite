package com.app.prod.utils.filters;

import com.app.prod.requests.enums.RequestStatus;
import lombok.experimental.SuperBuilder;
import org.jooq.Condition;

import java.util.List;
import java.util.UUID;

import static com.app.prod.utils.filters.Criteria.match;
import static com.app.prod.utils.filters.Criteria.matchEnum;
import static org.jooq.sources.Tables.REQUEST;

@SuperBuilder
public class RequestFilter extends PredicateFilter {
    Filter<RequestStatus> status;
    Filter<UUID> areaId;
    Filter<UUID> requestCreatorId;

    @Override
    public List<Condition> combineConditions() {
        return Criteria.of(
                matchEnum(REQUEST.STATUS, status),
                match(REQUEST.AREA_ID, areaId),
                match(REQUEST.USER_IN_NEED, requestCreatorId)
        );
    }
}
