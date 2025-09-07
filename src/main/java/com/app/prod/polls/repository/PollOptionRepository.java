package com.app.prod.polls.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.PollOptions;
import org.jooq.sources.tables.records.PollOptionsRecord;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class PollOptionRepository extends BaseJooqRepository<PollOptions, PollOptionsRecord, UUID> {
    protected PollOptionRepository(DSLContext dsl) {
        super(dsl, PollOptions.POLL_OPTIONS, PollOptions.POLL_OPTIONS.ID);
    }
}
