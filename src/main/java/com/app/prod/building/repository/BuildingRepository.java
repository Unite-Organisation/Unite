package com.app.prod.building.repository;

import com.app.prod.building.dto.BuildingResponse;
import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.Public;
import org.jooq.sources.tables.Building;
import org.jooq.sources.tables.records.BuildingRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static org.jooq.sources.tables.Building.BUILDING;

@Repository
public class BuildingRepository extends BaseJooqRepository<Building, BuildingRecord, UUID> {
    protected BuildingRepository(DSLContext dsl) {
        super(dsl, Building.BUILDING, Building.BUILDING.ID);
    }

    public List<BuildingResponse> getResidentsBuilding(UUID buildingId){
        return dslContext.select(BUILDING.fields())
                .from(BUILDING)
                .where(BUILDING.ID.eq(buildingId))
                .fetch(record -> new BuildingResponse(
                        record.get(BUILDING.ID),
                        record.get(BUILDING.NAME),
                        record.get(BUILDING.COUNTRY),
                        record.get(BUILDING.CITY),
                        record.get(BUILDING.STREET),
                        record.get(BUILDING.NUMBER),
                        record.get(BUILDING.AREA_ID)
                ));
    }
}
