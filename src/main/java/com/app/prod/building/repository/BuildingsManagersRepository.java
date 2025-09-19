package com.app.prod.building.repository;

import com.app.prod.building.dto.BuildingResponse;
import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.BuildingsManagers;
import org.jooq.sources.tables.records.BuildingsManagersRecord;
import org.jooq.sources.tables.records.BuildingsRecord;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

import static org.jooq.sources.Tables.BUILDINGS_MANAGERS;
import static org.jooq.sources.tables.Buildings.BUILDINGS;

@Repository
public class BuildingsManagersRepository extends BaseJooqRepository<BuildingsManagers, BuildingsManagersRecord, UUID> {
    protected BuildingsManagersRepository(DSLContext dsl) {
        super(dsl, BuildingsManagers.BUILDINGS_MANAGERS, BuildingsManagers.BUILDINGS_MANAGERS.ID);
    }

    public List<BuildingResponse> getManagersBuildings(UUID managerId){
        return dslContext.select(BUILDINGS.fields())
                .from(BUILDINGS)
                .leftJoin(table)
                .on(BUILDINGS.ID.eq(table.BUILDING_ID))
                .where(table.USER_ID.eq(managerId))
                .fetch(record -> new BuildingResponse(
                        record.get(BUILDINGS.ID),
                        record.get(BUILDINGS.NAME),
                        record.get(BUILDINGS.COUNTRY),
                        record.get(BUILDINGS.CITY),
                        record.get(BUILDINGS.STREET),
                        record.get(BUILDINGS.NUMBER),
                        record.get(BUILDINGS.AREA_ID)
                ));
    }

    public boolean managerManagesBuilding(UUID buildingId, UUID managerId){
        return dslContext.fetchExists(
                dslContext.selectFrom(BUILDINGS_MANAGERS)
                        .where(BUILDINGS_MANAGERS.USER_ID.eq(managerId))
                        .and(BUILDINGS_MANAGERS.BUILDING_ID.eq(buildingId))
        );
    }
}
