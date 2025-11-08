package com.app.prod.area.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.Area;
import org.jooq.sources.tables.records.AreaRecord;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import static org.jooq.sources.Tables.AREA;

@Repository
public class AreaRepository extends BaseJooqRepository<Area, AreaRecord, UUID> {
    protected AreaRepository(DSLContext dsl) {
        super(dsl, AREA, AREA.ID);
    }
}
