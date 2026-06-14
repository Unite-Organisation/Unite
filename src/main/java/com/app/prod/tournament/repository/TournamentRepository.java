package com.app.prod.tournament.repository;

import com.app.prod.tournament.dto.ParticipantResponse;
import com.app.prod.tournament.dto.TournamentResponse;
import com.app.prod.tournament.models.TournamentStatus;
import com.app.prod.tournament.models.TournamentType;
import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.sources.tables.Tournament;
import org.jooq.sources.tables.records.TournamentRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static org.jooq.Records.mapping;
import static org.jooq.impl.DSL.*;
import static org.jooq.sources.Tables.*;

@Repository
public class TournamentRepository extends BaseJooqRepository<Tournament, TournamentRecord, UUID> {
    protected TournamentRepository(DSLContext dsl) {
        super(dsl, TOURNAMENT, TOURNAMENT.ID);
    }

    public List<TournamentResponse> findAllByBuilidng(UUID buildingId, TournamentStatus status, UUID userId) {
        var condition = TOURNAMENT.BUILDING_ID.eq(buildingId);

        if (status != null) {
            condition = condition.and(TOURNAMENT.STATUS.eq(status.name()));
        }

        return dslContext.select(
                        TOURNAMENT.ID,
                        DSL.concat(APP_USER.FIRST_NAME, DSL.val(" "), APP_USER.LAST_NAME),
                        TOURNAMENT.NAME,
                        TOURNAMENT.DESCRIPTION,
                        TOURNAMENT.TEAM_SIZE,
                        multiset(
                                select(
                                        DSL.concat(APP_USER.FIRST_NAME, DSL.val(" "), APP_USER.LAST_NAME),
                                        APP_USER.USERNAME
                                )
                                        .from(TOURNAMENT_PARTICIPANT)
                                        .join(APP_USER).on(APP_USER.ID.eq(TOURNAMENT_PARTICIPANT.USER_ID))
                                        .where(TOURNAMENT_PARTICIPANT.TOURNAMENT_ID.eq(TOURNAMENT.ID))
                        ).as("participants").convertFrom(r -> r.map(record ->
                                new ParticipantResponse(
                                        record.value1(),
                                        record.value2()
                                ))),
                        TOURNAMENT.STATUS,
                        TOURNAMENT.TYPE,
                        TOURNAMENT.CREATED_BY
                )
                .from(TOURNAMENT)
                .join(APP_USER).on(APP_USER.ID.eq(TOURNAMENT.CREATED_BY))
                .where(condition)
                .fetch()
                .map(mapping((id, authorName, name, desc, teamSize, participants, tStatus, tType, tCreatedBy) ->
                        new TournamentResponse(
                                id,
                                authorName,
                                name,
                                desc,
                                teamSize,
                                participants,
                                TournamentStatus.valueOf(tStatus),
                                tType != null ? TournamentType.valueOf(tType) : null,
                                tCreatedBy.equals(userId)
                        )
                ));
    }
}
