package com.app.prod.polls.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.PollResult;
import org.jooq.sources.tables.records.PollResultRecord;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class PollResultRepository extends BaseJooqRepository<PollResult, PollResultRecord, UUID> {
    protected PollResultRepository(DSLContext dsl) {
        super(dsl, PollResult.POLL_RESULT, PollResult.POLL_RESULT.ID);
    }
}
