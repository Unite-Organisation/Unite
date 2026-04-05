package com.app.prod.area.repository;

import com.app.prod.area.dto.AreaInfoResponse;
import com.app.prod.area.enums.AreaType;
import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.Area;
import org.jooq.sources.tables.records.AreaRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static org.jooq.Records.mapping;
import static org.jooq.impl.DSL.*;
import static org.jooq.sources.Tables.*;

@Repository
public class AreaRepository extends BaseJooqRepository<Area, AreaRecord, UUID> {
    protected AreaRepository(DSLContext dsl) {
        super(dsl, AREA, AREA.ID);
    }

    public UUID getManagersArea(UUID id) {
        return dslContext.select(
                    AREA.ID
                )
                .from(BUILDING_MANAGER)
                .leftJoin(APP_USER).on(APP_USER.ID.eq(BUILDING_MANAGER.USER_ID))
                .leftJoin(BUILDING).on(BUILDING_MANAGER.BUILDING_ID.eq(BUILDING.ID))
                .leftJoin(AREA).on(BUILDING.AREA_ID.eq(AREA.ID))
                .where(APP_USER.ID.eq(id))
                .fetchAny(record -> record.get(AREA.ID));
    }

    public UUID getResidentArea(UUID id) {
        return dslContext.select(
                        AREA.ID
                )
                .from(APP_USER)
                .leftJoin(BUILDING).on(APP_USER.BUILDING_ID.eq(BUILDING.ID))
                .leftJoin(AREA).on(BUILDING.AREA_ID.eq(AREA.ID))
                .where(APP_USER.ID.eq(id))
                .fetchAny(record -> record.get(AREA.ID));
    }

    public UUID getAreaManager(UUID areaId) {
        return dslContext.select(
                BUILDING_MANAGER.USER_ID
        )
                .from(AREA)
                .leftJoin(BUILDING).on(BUILDING.AREA_ID.eq(AREA.ID))
                .leftJoin(BUILDING_MANAGER).on(BUILDING_MANAGER.BUILDING_ID.eq(BUILDING.ID))
                .where(AREA.ID.eq(areaId))
                .fetchAny(record -> record.get(BUILDING_MANAGER.USER_ID));
    }

    public List<AreaInfoResponse> findAllAreasWithBuildings() {
        return dslContext.select(
                        AREA.ID,
                        AREA.NAME,
                        AREA.COUNTRY,
                        AREA.CITY,
                        AREA.TYPE.convertFrom(AreaType::valueOf),
                        AREA.CREATED_AT,
                        field(
                                select(count(APP_USER.ID))
                                        .from(APP_USER)
                                        .join(BUILDING).on(APP_USER.BUILDING_ID.eq(BUILDING.ID))
                                        .where(BUILDING.AREA_ID.eq(AREA.ID))
                        ).as("usersNumber"),
                        multiset(
                                select(
                                        BUILDING.ID,
                                        BUILDING.NAME,
                                        BUILDING.STREET,
                                        BUILDING.NUMBER,
                                        field(
                                                select(count(APP_USER.ID))
                                                        .from(APP_USER)
                                                        .where(APP_USER.BUILDING_ID.eq(BUILDING.ID))
                                        ).as("usersNumber")
                                )
                                        .from(BUILDING)
                                        .where(BUILDING.AREA_ID.eq(AREA.ID))
                        ).as("buildings").convertFrom(r -> r.map(mapping(AreaInfoResponse.BuildingInfoResponse::new)))
                )
                .from(AREA)
                .fetch(mapping(AreaInfoResponse::new));
    }
}
