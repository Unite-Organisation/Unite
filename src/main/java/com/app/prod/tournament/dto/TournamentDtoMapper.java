package com.app.prod.tournament.dto;

import com.app.prod.tournament.models.Match;
import com.app.prod.tournament.models.MatchStatus;
import com.app.prod.tournament.models.Team;
import com.app.prod.tournament.models.Tournament;
import org.jooq.sources.tables.records.AppUserRecord;

import java.util.*;
import java.util.stream.Collectors;

public class TournamentDtoMapper {

    public static TournamentDto toDto(Tournament tournament, AppUserRecord user) {
        List<Match> allMatches = tournament.getMatches();
        if (allMatches == null || allMatches.isEmpty()) {
            return new TournamentDto(tournament.getId(), tournament.getName(), tournament.getCreatorId().equals(user.getId()), List.of());
        }

        Map<Integer, List<Match>> matchesByRound = allMatches.stream()
                .collect(Collectors.groupingBy(Match::getRoundNumber));

        int maxRound = matchesByRound.keySet().stream().max(Integer::compareTo).orElse(1);
        int minRound = matchesByRound.keySet().stream().min(Integer::compareTo).orElse(1);

        Map<Integer, List<MatchDto>> dtosByRound = matchesByRound.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().map(TournamentDtoMapper::mapToMatchDto).toList()
                ));

        Map<Integer, List<MatchDto>> orderedDtosByRound = new HashMap<>();

        List<MatchDto> finalMatches = dtosByRound.getOrDefault(maxRound, List.of()).stream()
                .sorted(Comparator.comparing(MatchDto::id))
                .toList();
        orderedDtosByRound.put(maxRound, finalMatches);

        for (int r = maxRound - 1; r >= minRound; r--) {
            List<MatchDto> nextRoundMatches = orderedDtosByRound.getOrDefault(r + 1, List.of());
            List<MatchDto> currentRoundMatches = dtosByRound.getOrDefault(r, List.of());

            List<MatchDto> sortedCurrentRound = new ArrayList<>();
            Set<UUID> processedMatchIds = new HashSet<>();

            for (MatchDto parentMatch : nextRoundMatches) {
                List<MatchDto> children = currentRoundMatches.stream()
                        .filter(m -> parentMatch.id().equals(m.nextMatchId()))
                        .sorted(Comparator.comparing(MatchDto::id))
                        .toList();

                sortedCurrentRound.addAll(children);
                children.forEach(c -> processedMatchIds.add(c.id()));
            }

            List<MatchDto> orphans = currentRoundMatches.stream()
                    .filter(m -> !processedMatchIds.contains(m.id()))
                    .sorted(Comparator.comparing(MatchDto::id))
                    .toList();
            sortedCurrentRound.addAll(orphans);

            orderedDtosByRound.put(r, sortedCurrentRound);
        }

        List<RoundDto> rounds = orderedDtosByRound.entrySet().stream()
                .map(entry -> new RoundDto(entry.getKey(), getRoundName(entry.getKey(), maxRound), entry.getValue()))
                .sorted(Comparator.comparing(RoundDto::roundNumber))
                .toList();

        return new TournamentDto(
                tournament.getId(),
                tournament.getName(),
                tournament.getCreatorId().equals(user.getId()),
                rounds
        );
    }

    private static MatchDto mapToMatchDto(Match match) {
        return new MatchDto(
                match.getId(),
                match.getNextMatchId(),
                match.getWinnerTeamId() != null ? MatchStatus.OVER : MatchStatus.PENDING,
                mapToTeamDto(match.getTeamA()),
                mapToTeamDto(match.getTeamB()),
                match.isSkip(),
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
        if (currentRound == maxRound - 2) return "Quarter-final";
        return "Round " + currentRound;
    }
}
