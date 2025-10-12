package com.app.prod.polls.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.PollWinner;
import org.jooq.sources.tables.records.PollWinnerRecord;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import static org.jooq.sources.Tables.POLL_WINNER;

@Repository
public class PollWinnerRepository extends BaseJooqRepository<PollWinner, PollWinnerRecord, UUID> {
    protected PollWinnerRepository(DSLContext dsl) {
        super(dsl, PollWinner.POLL_WINNER, POLL_WINNER.ID);
    }
}
