package com.app.prod.area.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.Area;
import org.jooq.sources.tables.records.AreaRecord;
import org.springframework.stereotype.Repository;

import java.util.UUID;

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
}
