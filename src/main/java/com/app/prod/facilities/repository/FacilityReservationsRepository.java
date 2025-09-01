package com.app.prod.facilities.repository;

import com.app.prod.facilities.dto.FacilityReservation;
import com.app.prod.facilities.enums.ReservationStatus;
import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.FacilitiesReservations;
import org.jooq.sources.tables.Users;
import org.jooq.sources.tables.records.FacilitiesReservationsRecord;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.jooq.sources.Tables.FACILITIES_RESERVATIONS;
import static org.jooq.sources.Tables.USERS;

@Repository
public class FacilityReservationsRepository extends BaseJooqRepository<FacilitiesReservations, FacilitiesReservationsRecord, UUID> {
    protected FacilityReservationsRepository(DSLContext dsl) {
        super(dsl, FacilitiesReservations.FACILITIES_RESERVATIONS, FacilitiesReservations.FACILITIES_RESERVATIONS.ID);
    }

    public List<FacilityReservation> getAvailability(UUID facilityId){
        return dslContext.select(
                    FACILITIES_RESERVATIONS.FACILITY_ID,
                    USERS.FIRST_NAME,
                    USERS.LAST_NAME,
                    FACILITIES_RESERVATIONS.START_TIME,
                    FACILITIES_RESERVATIONS.END_TIME
                )
                .from(FACILITIES_RESERVATIONS)
                .leftJoin(USERS).on(FACILITIES_RESERVATIONS.USER_ID.eq(USERS.ID))
                .where(FACILITIES_RESERVATIONS.FACILITY_ID.eq(facilityId))
                .and(FACILITIES_RESERVATIONS.STATUS.eq(ReservationStatus.RESERVED.name()))
                .orderBy(FACILITIES_RESERVATIONS.START_TIME)
                .fetch(record -> new FacilityReservation(
                        record.get(FACILITIES_RESERVATIONS.FACILITY_ID),
                        record.get(USERS.FIRST_NAME),
                        record.get(USERS.LAST_NAME),
                        record.get(FACILITIES_RESERVATIONS.START_TIME),
                        record.get(FACILITIES_RESERVATIONS.END_TIME)
                ));
    }

    public List<FacilitiesReservationsRecord> getOverlappingReservationsForFacility(UUID facilityId, LocalDateTime startTime, LocalDateTime endTime) {
        return dslContext.selectFrom(FACILITIES_RESERVATIONS)
                .where(FACILITIES_RESERVATIONS.FACILITY_ID.eq(facilityId))
                .and(FACILITIES_RESERVATIONS.START_TIME.lt(endTime))
                .and(FACILITIES_RESERVATIONS.END_TIME.gt(startTime))
                .fetch();
    }
}
