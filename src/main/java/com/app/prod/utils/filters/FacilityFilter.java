package com.app.prod.utils.filters;

import com.app.prod.facilities.enums.FacilityType;
import lombok.experimental.SuperBuilder;
import org.jooq.Condition;

import java.util.List;
import java.util.UUID;

import static com.app.prod.utils.filters.Criteria.match;
import static com.app.prod.utils.filters.Criteria.matchEnum;
import static com.app.prod.utils.filters.Criteria.required;
import static org.jooq.sources.Tables.FACILITY;

@SuperBuilder
public class FacilityFilter extends PredicateFilter {
    Filter<UUID> buildingId;
    Filter<FacilityType> facilityType;
    Filter<Boolean> requiresApproval;
    Filter<Integer> capacity;

    @Override
    public List<Condition> combineConditions() {
        return Criteria.of(
                Criteria.required(FACILITY.BUILDING_ID, buildingId),
                Criteria.matchEnum(FACILITY.TYPE, facilityType),
                Criteria.match(FACILITY.REQUIRES_APPROVAL, requiresApproval),
                Criteria.match(FACILITY.CAPACITY, capacity)
        );
    }

}
