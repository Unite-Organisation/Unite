package com.app.prod.polls.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.PollOption;
import org.jooq.sources.tables.records.PollOptionRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static org.jooq.sources.Tables.POLL_OPTION;

@Repository
public class PollOptionRepository extends BaseJooqRepository<PollOption, PollOptionRecord, UUID> {
    protected PollOptionRepository(DSLContext dsl) {
        super(dsl, PollOption.POLL_OPTION, PollOption.POLL_OPTION.ID);
    }

    public void addVoteForOption(UUID optionId){
        dslContext.update(POLL_OPTION)
                .set(POLL_OPTION.OPTION_VOTES, POLL_OPTION.OPTION_VOTES.plus(1))
                .where(POLL_OPTION.ID.eq(optionId))
                .execute();
    }

    public List<PollOptionRecord> getAllOptionsForPoll(UUID pollId){
        return dslContext.selectFrom(POLL_OPTION)
                .where(POLL_OPTION.POLL_ID.eq(pollId))
                .fetch();
    }
}
