package com.app.prod.area.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.Areas;
import org.jooq.sources.tables.records.AreasRecord;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class AreaRepository extends BaseJooqRepository<Areas, AreasRecord, UUID> {
    protected AreaRepository(DSLContext dsl) {
        super(dsl, Areas.AREAS, Areas.AREAS.ID);
    }
}
