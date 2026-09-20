package com.app.prod.facilities.repository;

import com.app.prod.facilities.enums.ReservationStatus;
import com.app.prod.utils.BaseJooqRepository;
import com.app.prod.utils.filters.FacilityReservationFilter;
import org.jooq.DSLContext;
import org.jooq.sources.tables.records.FacilityReservationRecord;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.jooq.sources.Tables.FACILITY_RESERVATION;
import static org.jooq.sources.Tables.APP_USER;

@Repository
public class FacilityReservationsRepository extends BaseJooqRepository<org.jooq.sources.tables.FacilityReservation, FacilityReservationRecord, UUID> {
    protected FacilityReservationsRepository(DSLContext dsl) {
        super(dsl, FACILITY_RESERVATION, FACILITY_RESERVATION.ID);
    }

    public List<com.app.prod.facilities.dto.FacilityReservation> findReservations(FacilityReservationFilter filter){
        return dslContext.select(
                    FACILITY_RESERVATION.ID,
                    APP_USER.FIRST_NAME,
                    APP_USER.LAST_NAME,
                    FACILITY_RESERVATION.START_TIME,
                    FACILITY_RESERVATION.END_TIME,
                    FACILITY_RESERVATION.STATUS
                )
                .from(FACILITY_RESERVATION)
                .leftJoin(APP_USER).on(FACILITY_RESERVATION.USER_ID.eq(APP_USER.ID))
                .where(filter.parseFilter())
                .orderBy(FACILITY_RESERVATION.START_TIME)
                .fetch(record -> new com.app.prod.facilities.dto.FacilityReservation(
                        record.get(FACILITY_RESERVATION.ID),
                        record.get(APP_USER.FIRST_NAME),
                        record.get(APP_USER.LAST_NAME),
                        record.get(FACILITY_RESERVATION.START_TIME),
                        record.get(FACILITY_RESERVATION.END_TIME),
                        ReservationStatus.valueOf(record.get(FACILITY_RESERVATION.STATUS))
                ));
    }

    public List<FacilityReservationRecord> getOverlappingReservationsForFacility(UUID facilityId, LocalDateTime startTime, LocalDateTime endTime) {
        return dslContext.selectFrom(FACILITY_RESERVATION)
                .where(FACILITY_RESERVATION.FACILITY_ID.eq(facilityId))
                .and(FACILITY_RESERVATION.START_TIME.lt(endTime))
                .and(FACILITY_RESERVATION.END_TIME.gt(startTime))
                .fetch();
    }
}
