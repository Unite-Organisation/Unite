package com.app.prod.polls.repository;

import com.app.prod.polls.dto.PollOptionVoteCount;
import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.sources.tables.PollVote;
import org.jooq.sources.tables.records.PollVoteRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static org.jooq.sources.Tables.POLL_OPTION;
import static org.jooq.sources.Tables.POLL_VOTE;

@Repository
public class PollVotesRepository extends BaseJooqRepository<PollVote, PollVoteRecord, UUID> {
    protected PollVotesRepository(DSLContext dsl) {
        super(dsl, PollVote.POLL_VOTE, PollVote.POLL_VOTE.ID);
    }

    public List<PollOptionVoteCount> countVotes(UUID pollId) {
        return dslContext.select(
                    POLL_VOTE.OPTION_ID,
                    DSL.count().as("count"),
                    POLL_OPTION.OPTION_TEXT
                )
                .from(POLL_VOTE)
                .leftJoin(POLL_OPTION).on(POLL_VOTE.OPTION_ID.eq(POLL_OPTION.ID))
                .where(POLL_VOTE.POLL_ID.eq(pollId))
                .groupBy(POLL_VOTE.OPTION_ID, POLL_OPTION.OPTION_TEXT)
                .orderBy(DSL.field("count").desc())
                .fetchInto(PollOptionVoteCount.class);
    }
}
