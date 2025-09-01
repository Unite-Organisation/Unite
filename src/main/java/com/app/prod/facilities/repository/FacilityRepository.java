package com.app.prod.facilities.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.Facilities;
import org.jooq.sources.tables.records.FacilitiesRecord;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class FacilityRepository extends BaseJooqRepository<Facilities, FacilitiesRecord, UUID> {
    protected FacilityRepository(DSLContext dsl) {
        super(dsl, Facilities.FACILITIES, Facilities.FACILITIES.ID);
    }
}
