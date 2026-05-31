package com.app.prod.tournament.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.TournamentTeam;
import org.jooq.sources.tables.records.TournamentTeamRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static org.jooq.sources.Tables.TOURNAMENT_TEAM;

@Repository
public class TournamentTeamRepository extends BaseJooqRepository<TournamentTeam, TournamentTeamRecord, UUID> {
    protected TournamentTeamRepository(DSLContext dsl) {
        super(dsl, TOURNAMENT_TEAM, TOURNAMENT_TEAM.ID);
    }

    public List<TournamentTeamRecord> findByTournamentId(UUID tournamentId) {
        return dslContext.selectFrom(TOURNAMENT_TEAM)
                .where(TOURNAMENT_TEAM.TOURNAMENT_ID.eq(tournamentId))
                .fetch();
    }
}
