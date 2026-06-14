
package com.app.prod.tournament;

import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.tournament.models.*;
import com.app.prod.tournament.repository.TeamMemberRepository;
import com.app.prod.tournament.repository.TournamentMatchRepository;
import com.app.prod.tournament.repository.TournamentRepository;
import com.app.prod.tournament.repository.TournamentTeamRepository;
import com.app.prod.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.AppUserRecord;
import org.jooq.sources.tables.records.TeamMemberRecord;
import org.jooq.sources.tables.records.TournamentMatchRecord;
import org.jooq.sources.tables.records.TournamentRecord;
import org.jooq.sources.tables.records.TournamentTeamRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TournamentMapper {

    private final TeamMemberRepository teamMemberRepository;
    private final TournamentMatchRepository tournamentMatchRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentTeamRepository tournamentTeamRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Optional<Tournament> fetchTournament(UUID tournamentId) {
        return tournamentRepository.findById(tournamentId)
                .map(this::mapToTournament);
    }

    @Transactional
    public void saveTournament(Tournament tournament) {
        TournamentRecord tournamentRecord = tournamentRepository.findById(tournament.getId()).orElseThrow(IllegalStateException::new);
        tournamentRecord.setStatus(tournament.getStatus().name());

        Set<Team> uniqueTeams = extractUniqueTeams(tournament);

        if (!uniqueTeams.isEmpty()) {
            List<TournamentTeamRecord> teamRecords = new ArrayList<>();
            List<TeamMemberRecord> memberRecords = new ArrayList<>();

            for (Team team : uniqueTeams) {
                TournamentTeamRecord tRecord = new TournamentTeamRecord();
                tRecord.setId(team.getId());
                tRecord.setTournamentId(tournament.getId());
                tRecord.setName(team.getName());
                teamRecords.add(tRecord);

                for (Participant member : team.getMembers()) {
                    TeamMemberRecord mRecord = new TeamMemberRecord();
                    mRecord.setId(UUID.randomUUID());
                    mRecord.setTeamId(team.getId());
                    mRecord.setUserId(member.getId());
                    memberRecords.add(mRecord);
                }
            }

            tournamentTeamRepository.insertMany(teamRecords);
            teamMemberRepository.insertMany(memberRecords);
        }

        if (tournament.getMatches() != null && !tournament.getMatches().isEmpty()) {
            List<TournamentMatchRecord> matchRecords = new ArrayList<>();

            for (Match match : tournament.getMatches()) {
                TournamentMatchRecord mRecord = new TournamentMatchRecord();
                mRecord.setId(match.getId());
                mRecord.setTournamentId(match.getTournamentId());
                mRecord.setNextMatchId(match.getNextMatchId());
                mRecord.setRoundNumber(match.getRoundNumber());

                mRecord.setTeamAId(match.getTeamA() != null ? match.getTeamA().getId() : null);
                mRecord.setTeamBId(match.getTeamB() != null ? match.getTeamB().getId() : null);
                mRecord.setWinnerTeamId(match.getWinnerTeamId());
                mRecord.setIsSkip(match.isSkip());

                matchRecords.add(mRecord);
            }

            tournamentMatchRepository.insertMany(matchRecords);
        }

        tournamentRepository.update(tournamentRecord);
    }

    private Tournament mapToTournament(TournamentRecord record) {
        UUID tournamentId = record.getId();
        Map<UUID, Team> teamsById = loadTeamsById(tournamentId);
        List<Match> matches = tournamentMatchRepository.findByTournamentId(tournamentId).stream()
                .map(matchRecord -> mapToMatch(matchRecord, teamsById))
                .toList();
        return new Tournament(tournamentId, record.getName(), matches, TournamentStatus.valueOf(record.getStatus()), record.getCreatedBy());
    }

    private Map<UUID, Team> loadTeamsById(UUID tournamentId) {
        List<TournamentTeamRecord> teamRecords = tournamentTeamRepository.findByTournamentId(tournamentId);
        if (teamRecords.isEmpty()) {
            return Map.of();
        }

        List<UUID> teamIds = teamRecords.stream().map(TournamentTeamRecord::getId).toList();
        List<TeamMemberRecord> memberRecords = teamMemberRepository.findByTeamIds(teamIds);

        Set<UUID> userIds = memberRecords.stream()
                .map(TeamMemberRecord::getUserId)
                .collect(Collectors.toSet());
        Map<UUID, AppUserRecord> usersById = userRepository.findByIds(new ArrayList<>(userIds)).stream()
                .collect(Collectors.toMap(AppUserRecord::getId, user -> user));

        Map<UUID, List<Participant>> membersByTeamId = new HashMap<>();
        for (TeamMemberRecord memberRecord : memberRecords) {
            AppUserRecord user = usersById.get(memberRecord.getUserId());
            String username = user != null ? user.getUsername() : "";
            membersByTeamId
                    .computeIfAbsent(memberRecord.getTeamId(), ignored -> new ArrayList<>())
                    .add(new Participant(memberRecord.getUserId(), username));
        }

        Map<UUID, Team> teamsById = new HashMap<>();
        for (TournamentTeamRecord teamRecord : teamRecords) {
            List<Participant> members = membersByTeamId.get(teamRecord.getId());
            if (members != null && !members.isEmpty()) {
                teamsById.put(teamRecord.getId(), new Team(teamRecord.getId(), teamRecord.getName(), members));
            }
        }
        return teamsById;
    }

    private Match mapToMatch(TournamentMatchRecord matchRecord, Map<UUID, Team> teamsById) {
        Match match = new Match(
                matchRecord.getId(),
                matchRecord.getTournamentId(),
                matchRecord.getNextMatchId(),
                matchRecord.getRoundNumber()
        );

        if (matchRecord.getTeamAId() != null) {
            match.setTeamA(teamsById.get(matchRecord.getTeamAId()));
        }
        if (matchRecord.getTeamBId() != null) {
            match.setTeamB(teamsById.get(matchRecord.getTeamBId()));
        }
        match.setWinnerTeamId(matchRecord.getWinnerTeamId());
        match.setSkip(matchRecord.getIsSkip());
        if (matchRecord.getStatus() != null) {
            match.setMatchStatus(MatchStatus.valueOf(matchRecord.getStatus()));
        }
        return match;
    }

    private Set<Team> extractUniqueTeams(Tournament tournament) {
        Set<Team> teams = new HashSet<>();
        if (tournament.getMatches() == null) {
            return teams;
        }

        for (Match match : tournament.getMatches()) {
            if (match.getTeamA() != null) {
                teams.add(match.getTeamA());
            }
            if (match.getTeamB() != null) {
                teams.add(match.getTeamB());
            }
        }
        return teams;
    }
}
