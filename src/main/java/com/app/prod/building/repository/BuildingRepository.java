package com.app.prod.building.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.Public;
import org.jooq.sources.tables.Building;
import org.jooq.sources.tables.records.BuildingRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class BuildingRepository extends BaseJooqRepository<Building, BuildingRecord, UUID> {
    protected BuildingRepository(DSLContext dsl) {
        super(dsl, Building.BUILDING, Building.BUILDING.ID);
    }
}
