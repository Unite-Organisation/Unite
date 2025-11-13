package com.app.prod.requests.repository;

import com.app.prod.requests.enums.RequestStatus;
import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.Request;
import org.jooq.sources.tables.records.RequestRecord;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import static org.jooq.sources.Tables.REQUEST;

@Repository
public class RequestRepository extends BaseJooqRepository<Request, RequestRecord, UUID> {
    protected RequestRepository(DSLContext dsl) {
        super(dsl, REQUEST, REQUEST.ID);
    }

    public void updateStatus(RequestStatus status, UUID id){
        dslContext.update(REQUEST)
                .set(REQUEST.STATUS, status.name())
                .where(REQUEST.ID.eq(id))
                .execute();
    }
}
