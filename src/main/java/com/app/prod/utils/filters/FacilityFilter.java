package com.app.prod.utils.filters;

import com.app.prod.facilities.enums.FacilityType;
import lombok.Builder;
import org.jooq.Condition;

import java.util.List;
import java.util.UUID;

import static com.app.prod.utils.filters.Criteria.match;
import static com.app.prod.utils.filters.Criteria.matchEnum;
import static com.app.prod.utils.filters.Criteria.required;
import static org.jooq.sources.Tables.FACILITY;

@Builder
public class FacilityFilter implements PredicateFilter {
    Filter<UUID> buildingId;
    Filter<FacilityType> facilityType;
    Filter<Boolean> requiresApproval;
    Filter<Integer> capacity;

    @Override
    public List<Condition> combineConditions() {
        return Criteria.of(
                required(FACILITY.BUILDING_ID, buildingId),
                matchEnum(FACILITY.TYPE, facilityType),
                match(FACILITY.REQUIRES_APPROVAL, requiresApproval),
                match(FACILITY.CAPACITY, capacity)
        );
    }

}
