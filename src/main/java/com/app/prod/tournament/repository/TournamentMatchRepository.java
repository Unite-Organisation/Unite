package com.app.prod.tournament.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.TournamentMatch;
import org.jooq.sources.tables.records.TournamentMatchRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static org.jooq.sources.Tables.TOURNAMENT_MATCH;

@Repository
public class TournamentMatchRepository extends BaseJooqRepository<TournamentMatch, TournamentMatchRecord, UUID> {
    protected TournamentMatchRepository(DSLContext dsl) {
        super(dsl, TOURNAMENT_MATCH, TOURNAMENT_MATCH.ID);
    }

    public List<TournamentMatchRecord> findByTournamentId(UUID tournamentId) {
        return dslContext.selectFrom(TOURNAMENT_MATCH)
                .where(TOURNAMENT_MATCH.TOURNAMENT_ID.eq(tournamentId))
                .fetch();
    }
}
