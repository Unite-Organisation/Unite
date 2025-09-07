package com.app.prod.polls.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.PollVotes;
import org.jooq.sources.tables.records.PollVotesRecord;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class PollVotesRepository extends BaseJooqRepository<PollVotes, PollVotesRecord, UUID> {
    protected PollVotesRepository(DSLContext dsl) {
        super(dsl, PollVotes.POLL_VOTES, PollVotes.POLL_VOTES.ID);
    }
}
