package com.app.prod.offering.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.Offering;
import org.jooq.sources.tables.records.OfferingRecord;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import static org.jooq.sources.Tables.OFFERING;

@Repository
public class OfferingRepository extends BaseJooqRepository<Offering, OfferingRecord, UUID> {
    protected OfferingRepository(DSLContext dsl) {
        super(dsl, OFFERING, OFFERING.ID);
    }

}
