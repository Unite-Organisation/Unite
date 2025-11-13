package com.app.prod.utils.filters;

import com.app.prod.requests.enums.RequestStatus;
import lombok.Builder;
import org.jooq.Condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.sources.Tables.REQUEST;

@Builder
public class RequestFilter implements PredicateFilter{
    public Optional<RequestStatus> status;
    public Optional<UUID> areaId;
    public Optional<UUID> requestCreatorId;

    @Override
    public List<Condition> combineConditions() {
        List<Condition> conditionList = new ArrayList<>();

        status.ifPresent(r -> conditionList.add(REQUEST.STATUS.eq(r.name())));
        areaId.ifPresent(r -> conditionList.add(REQUEST.AREA_ID.eq(r)));
        requestCreatorId.ifPresent(r -> conditionList.add(REQUEST.USER_IN_NEED.eq(r)));

        return conditionList;
    }
}
