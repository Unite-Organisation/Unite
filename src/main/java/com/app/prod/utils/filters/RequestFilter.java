package com.app.prod.utils.filters;

import com.app.prod.requests.enums.RequestStatus;
import lombok.Builder;
import org.jooq.Condition;

import java.util.List;
import java.util.UUID;

import static com.app.prod.utils.filters.Criteria.match;
import static com.app.prod.utils.filters.Criteria.matchEnum;
import static org.jooq.sources.Tables.REQUEST;

@Builder
public class RequestFilter implements PredicateFilter {
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
