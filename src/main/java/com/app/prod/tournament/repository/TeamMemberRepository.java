package com.app.prod.tournament.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.TeamMember;
import org.jooq.sources.tables.Tournament;
import org.jooq.sources.tables.records.TeamMemberRecord;
import org.jooq.sources.tables.records.TournamentRecord;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.jooq.sources.Tables.TEAM_MEMBER;

@Repository
public class TeamMemberRepository extends BaseJooqRepository<TeamMember, TeamMemberRecord, UUID> {
    protected TeamMemberRepository(DSLContext dsl) {
        super(dsl, TEAM_MEMBER, TEAM_MEMBER.ID);
    }

    public List<TeamMemberRecord> findByTeamIds(Collection<UUID> teamIds) {
        if (teamIds.isEmpty()) {
            return List.of();
        }
        return dslContext.selectFrom(TEAM_MEMBER)
                .where(TEAM_MEMBER.TEAM_ID.in(teamIds))
                .fetch();
    }
}
