package com.app.prod.polls.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.Polls;
import org.jooq.sources.tables.records.PollsRecord;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class PollRepository extends BaseJooqRepository<Polls, PollsRecord, UUID> {
    protected PollRepository(DSLContext dsl) {
        super(dsl, Polls.POLLS, Polls.POLLS.ID);
    }
}
