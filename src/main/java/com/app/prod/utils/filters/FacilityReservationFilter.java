package com.app.prod.utils.filters;

import lombok.Builder;
import org.jooq.Condition;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.app.prod.utils.filters.Criteria.always;
import static com.app.prod.utils.filters.Criteria.match;
import static com.app.prod.utils.filters.Criteria.required;
import static org.jooq.sources.Tables.FACILITY_RESERVATION;

@Builder
public class FacilityReservationFilter implements PredicateFilter {
    Filter<UUID> facilityId;
    Filter<LocalDate> day;
    Filter<UUID> userId;

    @Override
    public List<Condition> combineConditions() {
        LocalDate requestedDay = getDay();

        return Criteria.of(
                required(FACILITY_RESERVATION.FACILITY_ID, facilityId),
                always(FACILITY_RESERVATION.START_TIME.lt(requestedDay.plusDays(1).atStartOfDay())),
                always(FACILITY_RESERVATION.END_TIME.gt(requestedDay.atStartOfDay())),
                match(FACILITY_RESERVATION.USER_ID, userId)
        );
    }

    public LocalDate getDay() {
        return day == null ? Filter.<LocalDate>empty().require("day") : day.require("day");
    }

}
