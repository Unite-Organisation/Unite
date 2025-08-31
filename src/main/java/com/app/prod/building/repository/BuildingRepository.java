package com.app.prod.building.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.Public;
import org.jooq.sources.tables.Buildings;
import org.jooq.sources.tables.records.BuildingsRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class BuildingRepository extends BaseJooqRepository<Buildings, BuildingsRecord, UUID> {
    protected BuildingRepository(DSLContext dsl) {
        super(dsl, Buildings.BUILDINGS, Buildings.BUILDINGS.ID);
    }
}
