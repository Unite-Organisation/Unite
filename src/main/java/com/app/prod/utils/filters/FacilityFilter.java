package com.app.prod.utils.filters;

import com.app.prod.facilities.enums.FacilityType;
import lombok.Builder;
import org.jooq.Condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.sources.Tables.FACILITY;

@Builder
public class FacilityFilter implements PredicateFilter {
    UUID buildingId;
    Optional<FacilityType> facilityType;
    Optional<Boolean> requiresApproval;
    ComparisonFilter<Integer> capacity;

    @Override
    public List<Condition> combineConditions() {
        List<Condition> conditionList = new ArrayList<>();

        conditionList.add(FACILITY.BUILDING_ID.eq(buildingId));
        facilityType.ifPresent(type -> conditionList.add(FACILITY.TYPE.eq(type.name())));
        requiresApproval.ifPresent(flag -> conditionList.add(FACILITY.REQUIRES_APPROVAL.eq(flag)));
        capacity.toCondition(FACILITY.CAPACITY).ifPresent(conditionList::add);

        return conditionList;
    }

}
