package com.app.prod.requests.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.RequestDonor;
import org.jooq.sources.tables.records.RequestDonorRecord;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import static org.jooq.sources.Tables.REQUEST_DONOR;

@Repository
public class RequestDonorRepository extends BaseJooqRepository<RequestDonor, RequestDonorRecord, UUID> {
    protected RequestDonorRepository(DSLContext dsl) {
        super(dsl, REQUEST_DONOR, REQUEST_DONOR.ID);
    }

    public boolean userIsDonorForRequest(UUID userId, UUID requestId) {
        return dslContext.selectOne()
                .from(REQUEST_DONOR)
                .where(REQUEST_DONOR.DONOR_ID.eq(userId))
                .and(REQUEST_DONOR.REQUEST_ID.eq(requestId))
                .fetch()
                .isNotEmpty();
    }
}
