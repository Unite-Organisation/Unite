package com.app.prod.polls.repository;

import com.app.prod.polls.dto.PollOptionVoteCount;
import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.sources.tables.PollVotes;
import org.jooq.sources.tables.records.PollVotesRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static org.jooq.sources.Tables.POLL_VOTES;

@Repository
public class PollVotesRepository extends BaseJooqRepository<PollVotes, PollVotesRecord, UUID> {
    protected PollVotesRepository(DSLContext dsl) {
        super(dsl, PollVotes.POLL_VOTES, PollVotes.POLL_VOTES.ID);
    }

    public List<PollOptionVoteCount> countVotes(UUID pollId) {
        return dslContext.select(
                    POLL_VOTES.OPTION_ID,
                    DSL.count().as("count")
                )
                .from(POLL_VOTES)
                .where(POLL_VOTES.POLL_ID.eq(pollId))
                .groupBy(POLL_VOTES.OPTION_ID)
                .orderBy(DSL.field("count").desc())
                .fetchInto(PollOptionVoteCount.class);
    }
}
