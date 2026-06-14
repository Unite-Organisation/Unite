package com.app.prod.tournament.models;

import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class TournamentFactory {

    public Tournament createTournament(String name, UUID tournamentId, List<Participant> participants, int teamSize, UUID creatorId) {
        validateTournament(participants.size(), teamSize);

        List<Team> teams = groupParticipantsIntoTeams(participants, teamSize);
        int totalTeams = teams.size();

        if (totalTeams < 2) {
            throw new IllegalArgumentException("Too little teams, to create tournament (minimum 2 teams)");
        }

        int nextPowerOfTwo = Integer.highestOneBit(totalTeams - 1) << 1;
        int totalRounds = (int) (Math.log(nextPowerOfTwo) / Math.log(2));

        List<Match> allMatches = new ArrayList<>();

        createBinaryTreeStructure(tournamentId, null, totalRounds, allMatches);

        List<Match> firstRoundMatches = allMatches.stream()
                .filter(m -> m.getRoundNumber() == 1)
                .toList();

        Collections.shuffle(teams);

        for (int i = 0; i < firstRoundMatches.size(); i++) {
            firstRoundMatches.get(i).setTeamA(teams.get(i));
        }

        int remainingTeamsStartIndex = firstRoundMatches.size();
        for (int i = 0; i < teams.size() - remainingTeamsStartIndex; i++) {
            firstRoundMatches.get(i).setTeamB(teams.get(remainingTeamsStartIndex + i));
        }

        for (Match match : firstRoundMatches) {
            if (match.getTeamA() != null && match.getTeamB() == null) {
//                match.setWinnerTeamId(match.getTeamA().getId());
                match.setSkip(true);

                if (match.getNextMatchId() != null) {
                    Match nextMatch = findMatchById(allMatches, match.getNextMatchId());
                    promoteTeamToNextMatch(nextMatch, match.getTeamA());
                }
            }
        }

        return new Tournament(tournamentId, name, allMatches, TournamentStatus.CLOSED, creatorId);
    }

    private void validateTournament(int numberOfParticipants, int teamSize) {
        if (numberOfParticipants == 0) {
            throw new IllegalArgumentException("Illegal number of participants");
        }
        if (teamSize <= 0) {
            throw new IllegalArgumentException("Team size must be greater than zero");
        }
        if (teamSize > 1 && numberOfParticipants % teamSize != 0) {
            throw new IllegalArgumentException(String.format("Failed to create tournament. Number of participants (%d) must be divided by team size (%d).", numberOfParticipants, teamSize));
        }
    }

    private List<Team> groupParticipantsIntoTeams(List<Participant> participants, int teamSize) {
        List<Team> teams = new ArrayList<>();
        int teamCounter = 1;

        for (int i = 0; i < participants.size(); i += teamSize) {
            List<Participant> teamMembers = participants.subList(i, i + teamSize);

            String teamName = teamSize == 1
                    ? teamMembers.getFirst().getUsername()
                    : "Team " + teamCounter++;

            teams.add(new Team(UUID.randomUUID(), teamName, new ArrayList<>(teamMembers)));
        }
        return teams;
    }

    private void createBinaryTreeStructure(UUID tournamentId, UUID nextMatchId, int currentRound, List<Match> allMatches) {
        UUID matchId = UUID.randomUUID();
        Match match = new Match(matchId, tournamentId, nextMatchId, currentRound);
        allMatches.add(match);

        if (currentRound > 1) {
            createBinaryTreeStructure(tournamentId, matchId, currentRound - 1, allMatches);
            createBinaryTreeStructure(tournamentId, matchId, currentRound - 1, allMatches);
        }
    }

    private void promoteTeamToNextMatch(Match nextMatch, Team team) {
        if (nextMatch.getTeamA() == null) {
            nextMatch.setTeamA(team);
        } else if (nextMatch.getTeamB() == null) {
            nextMatch.setTeamB(team);
        } else {
            throw new IllegalStateException();
        }
    }

    private Match findMatchById(List<Match> matches, UUID id) {
        return matches.stream()
                .filter(m -> m.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Match not found: " + id));
    }
}
