package com.app.prod.tournament.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.TournamentParticipant;
import org.jooq.sources.tables.records.TournamentParticipantRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static org.jooq.sources.Tables.TOURNAMENT_PARTICIPANT;

@Repository
public class TournamentParticipantRepository extends BaseJooqRepository<TournamentParticipant, TournamentParticipantRecord, UUID> {
    protected TournamentParticipantRepository(DSLContext dsl) {
        super(dsl, TOURNAMENT_PARTICIPANT, TOURNAMENT_PARTICIPANT.ID);
    }

    public List<TournamentParticipantRecord> findByTournament(UUID tournamentId) {
        return dslContext.selectFrom(TOURNAMENT_PARTICIPANT)
                .where(TOURNAMENT_PARTICIPANT.TOURNAMENT_ID.eq(tournamentId))
                .fetch();
    }

}
