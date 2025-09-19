package com.app.prod.polls.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.PollOptions;
import org.jooq.sources.tables.records.PollOptionsRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static org.jooq.sources.Tables.POLL_OPTIONS;

@Repository
public class PollOptionRepository extends BaseJooqRepository<PollOptions, PollOptionsRecord, UUID> {
    protected PollOptionRepository(DSLContext dsl) {
        super(dsl, PollOptions.POLL_OPTIONS, PollOptions.POLL_OPTIONS.ID);
    }

    public void addVoteForOption(UUID optionId){
        dslContext.update(POLL_OPTIONS)
                .set(POLL_OPTIONS.OPTION_VOTES, POLL_OPTIONS.OPTION_VOTES.plus(1))
                .where(POLL_OPTIONS.ID.eq(optionId))
                .execute();
    }

    public List<PollOptionsRecord> getAllOptionsForPoll(UUID pollId){
        return dslContext.selectFrom(POLL_OPTIONS)
                .where(POLL_OPTIONS.POLL_ID.eq(pollId))
                .fetch();
    }
}
