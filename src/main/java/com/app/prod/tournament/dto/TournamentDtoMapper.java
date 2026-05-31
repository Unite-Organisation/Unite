package com.app.prod.tournament.dto;

import com.app.prod.tournament.models.Match;
import com.app.prod.tournament.models.MatchStatus;
import com.app.prod.tournament.models.Team;
import com.app.prod.tournament.models.Tournament;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TournamentDtoMapper {

    public static TournamentDto toDto(Tournament tournament) {
        Map<Integer, List<Match>> matchesByRound = tournament.getMatches().stream()
                .collect(Collectors.groupingBy(Match::getRoundNumber));

        int maxRound = matchesByRound.keySet().stream().max(Integer::compareTo).orElse(1);

        List<RoundDto> rounds = matchesByRound.entrySet().stream()
                .map(entry -> {
                    int roundNum = entry.getKey();
                    List<MatchDto> matchDtos = entry.getValue().stream()
                            .map(TournamentDtoMapper::mapToMatchDto)
                            .sorted(Comparator.comparing(MatchDto::id))
                            .toList();

                    return new RoundDto(roundNum, getRoundName(roundNum, maxRound), matchDtos);
                })
                .sorted(Comparator.comparing(RoundDto::roundNumber))
                .toList();

        return new TournamentDto(tournament.getId(), tournament.getName(), rounds);
    }

    private static MatchDto mapToMatchDto(Match match) {
        return new MatchDto(
                match.getId(),
                match.getNextMatchId(),
                match.getWinnerTeamId() != null ? MatchStatus.OVER : MatchStatus.PENDING,
                mapToTeamDto(match.getTeamA()),
                mapToTeamDto(match.getTeamB()),
                match.getWinnerTeamId()
        );
    }

    private static TeamDto mapToTeamDto(Team team) {
        if (team == null) return null;

        List<ParticipantDto> members = team.getMembers().stream()
                .map(p -> new ParticipantDto(p.getId(), p.getUsername()))
                .toList();

        return new TeamDto(team.getId(), team.getName(), members);
    }

    private static String getRoundName(int currentRound, int maxRound) {
        if (currentRound == maxRound) return "Final";
        if (currentRound == maxRound - 1) return "Semi-final";
        if (currentRound == maxRound - 2) return "Quoter-final";
        return "Round " + currentRound;
    }

}
