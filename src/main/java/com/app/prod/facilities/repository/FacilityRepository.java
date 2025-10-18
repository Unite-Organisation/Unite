package com.app.prod.facilities.repository;

import com.app.prod.facilities.dto.FacilityResponse;
import com.app.prod.utils.BaseJooqRepository;
import com.app.prod.utils.validators.Validate;
import org.jooq.DSLContext;
import org.jooq.sources.tables.Facilities;
import org.jooq.sources.tables.records.FacilitiesRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static org.jooq.sources.Tables.*;

@Repository
public class FacilityRepository extends BaseJooqRepository<Facilities, FacilitiesRecord, UUID> {
    protected FacilityRepository(DSLContext dsl) {
        super(dsl, Facilities.FACILITIES, Facilities.FACILITIES.ID);
    }

    public List<FacilityResponse> getFacilitiesForBuilding(UUID buildingId){
        return dslContext.select(
                FACILITIES.ID,
                FACILITIES.NAME,
                FACILITIES.TYPE,
                FACILITIES.CAPACITY,
                FACILITIES.LOCATION,
                FACILITIES.REQUIRES_APPROVAL
        )
                .from(FACILITIES)
                .where(FACILITIES.BUILDING_ID.eq(buildingId))
                .fetch(r -> new FacilityResponse(
                        r.get(FACILITIES.ID),
                        r.get(FACILITIES.NAME),
                        r.get(FACILITIES.TYPE),
                        r.get(FACILITIES.CAPACITY),
                        r.get(FACILITIES.LOCATION),
                        r.get(FACILITIES.REQUIRES_APPROVAL)
                ));
    }

    public UUID fetchManagerIdManagingFacility(UUID facilityId) {
        return dslContext.select(BUILDINGS_MANAGERS.USER_ID)
                .from(FACILITIES)
                .leftJoin(BUILDINGS).on(FACILITIES.BUILDING_ID.eq(BUILDINGS.ID))
                .leftJoin(BUILDINGS_MANAGERS).on(BUILDINGS.ID.eq(BUILDINGS_MANAGERS.BUILDING_ID))
                .where(FACILITIES.ID.eq(facilityId))
                .fetchOne(BUILDINGS_MANAGERS.USER_ID);
    }
}
