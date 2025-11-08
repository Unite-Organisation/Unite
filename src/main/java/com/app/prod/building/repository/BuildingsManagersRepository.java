package com.app.prod.building.repository;

import com.app.prod.building.dto.BuildingResponse;
import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.BuildingManager;
import org.jooq.sources.tables.records.BuildingManagerRecord;
import org.jooq.sources.tables.records.BuildingRecord;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

import static org.jooq.sources.Tables.BUILDING_MANAGER;
import static org.jooq.sources.tables.Building.BUILDING;

@Repository
public class BuildingsManagersRepository extends BaseJooqRepository<BuildingManager, BuildingManagerRecord, UUID> {
    protected BuildingsManagersRepository(DSLContext dsl) {
        super(dsl, BuildingManager.BUILDING_MANAGER, BuildingManager.BUILDING_MANAGER.ID);
    }

    public List<BuildingResponse> getManagersBuildings(UUID managerId){
        return dslContext.select(BUILDING.fields())
                .from(BUILDING)
                .leftJoin(table)
                .on(BUILDING.ID.eq(table.BUILDING_ID))
                .where(table.USER_ID.eq(managerId))
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

    public boolean managerManagesBuilding(UUID buildingId, UUID managerId){
        return dslContext.fetchExists(
                dslContext.selectFrom(BUILDING_MANAGER)
                        .where(BUILDING_MANAGER.USER_ID.eq(managerId))
                        .and(BUILDING_MANAGER.BUILDING_ID.eq(buildingId))
        );
    }

    public UUID fetchManagerIdForBuilding(UUID buildingId){
        return dslContext.select(BUILDING_MANAGER.USER_ID)
                .from(BUILDING_MANAGER)
                .where(BUILDING_MANAGER.BUILDING_ID.eq(buildingId))
                .fetchOne(BUILDING_MANAGER.USER_ID);
    }
}
