package com.app.prod.facilities.repository;

import com.app.prod.facilities.dto.FacilityResponse;
import com.app.prod.utils.BaseJooqRepository;
import com.app.prod.utils.validators.Validate;
import org.jooq.DSLContext;
import org.jooq.sources.tables.Facility;
import org.jooq.sources.tables.records.FacilityRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static org.jooq.sources.Tables.*;

@Repository
public class FacilityRepository extends BaseJooqRepository<Facility, FacilityRecord, UUID> {
    protected FacilityRepository(DSLContext dsl) {
        super(dsl, Facility.FACILITY, Facility.FACILITY.ID);
    }

    public List<FacilityResponse> getFacilities(UUID userId){
        return dslContext.select(
                FACILITY.ID,
                FACILITY.NAME,
                FACILITY.TYPE,
                FACILITY.CAPACITY,
                FACILITY.LOCATION,
                FACILITY.REQUIRES_APPROVAL
        )
                .from(APP_USER)
                .leftJoin(BUILDING_MANAGER).on(APP_USER.ID.eq(BUILDING_MANAGER.USER_ID))
                .innerJoin(BUILDING).on(
                        BUILDING.ID.eq(APP_USER.BUILDING_ID)
                                .or(BUILDING.ID.eq(BUILDING_MANAGER.BUILDING_ID))
                )
                .join(FACILITY).on(BUILDING.ID.eq(FACILITY.BUILDING_ID))
                .where(APP_USER.ID.eq(userId))
                .fetch(r -> new FacilityResponse(
                        r.get(FACILITY.ID),
                        r.get(FACILITY.NAME),
                        r.get(FACILITY.TYPE),
                        r.get(FACILITY.CAPACITY),
                        r.get(FACILITY.LOCATION),
                        r.get(FACILITY.REQUIRES_APPROVAL)
                ));
    }

    public UUID fetchManagerIdManagingFacility(UUID facilityId) {
        return dslContext.select(BUILDING_MANAGER.USER_ID)
                .from(FACILITY)
                .leftJoin(BUILDING).on(FACILITY.BUILDING_ID.eq(BUILDING.ID))
                .leftJoin(BUILDING_MANAGER).on(BUILDING.ID.eq(BUILDING_MANAGER.BUILDING_ID))
                .where(FACILITY.ID.eq(facilityId))
                .fetchOne(BUILDING_MANAGER.USER_ID);
    }
}
