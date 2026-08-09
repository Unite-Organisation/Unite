package com.app.prod.utils.filters;

import lombok.Builder;
import org.jooq.Condition;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.sources.Tables.FACILITY_RESERVATION;

@Builder
public class FacilityReservationFilter implements PredicateFilter {
    UUID facilityId;
    LocalDate day;
    Optional<UUID> userId;

    @Override
    public List<Condition> combineConditions() {
        List<Condition> conditionList = new ArrayList<>();

        conditionList.add(FACILITY_RESERVATION.FACILITY_ID.eq(facilityId));
        conditionList.add(FACILITY_RESERVATION.START_TIME.lt(day.plusDays(1).atStartOfDay()));
        conditionList.add(FACILITY_RESERVATION.END_TIME.gt(day.atStartOfDay()));
        userId.ifPresent(id -> conditionList.add(FACILITY_RESERVATION.USER_ID.eq(id)));

        return conditionList;
    }

    public LocalDate getDay() {
        return day;
    }

}
