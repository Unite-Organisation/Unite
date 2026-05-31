package com.app.prod.tournament;

import com.app.prod.tournament.models.Match;
import com.app.prod.tournament.models.Participant;
import com.app.prod.tournament.models.Tournament;
import com.app.prod.tournament.models.TournamentFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TournamentFactoryTest {

    private TournamentFactory tournamentFactory;

    @BeforeEach
    void setUp() {
        tournamentFactory = new TournamentFactory();
    }

    @Test
    @DisplayName("Powinien stworzyć idealną drabinkę dla 8 graczy (1v1) - brak wolnych losów")
    void shouldCreatePerfectBracketFor8Players1v1() {
        // given
        List<Participant> participants = generateParticipants(8);

        // when
        Tournament tournament = tournamentFactory.createTournament("Turniej Ping-Ponga", participants, 1);

        // then
        assertThat(tournament).isNotNull();
        assertThat(tournament.getId()).isNotNull();
        assertThat(tournament.getMatches()).hasSize(7);

        List<Match> round1Matches = getMatchesByRound(tournament, 1);
        assertThat(round1Matches).hasSize(4);

        for (Match match : round1Matches) {
            assertThat(match.getTeamA()).isNotNull();
            assertThat(match.getTeamB()).isNotNull();
            assertThat(match.getWinnerTeamId()).isNull();
        }
    }

    @Test
    @DisplayName("Powinien stworzyć drabinkę z 3 wolnymi losami dla 5 graczy (1v1)")
    void shouldCreateBracketWith3ByesFor5Players1v1() {
        // given
        List<Participant> participants = generateParticipants(5);

        // when
        Tournament tournament = tournamentFactory.createTournament("Turniej Szachowy", participants, 1);

        // then
        assertThat(tournament.getMatches()).hasSize(7);

        List<Match> round1Matches = getMatchesByRound(tournament, 1);
        assertThat(round1Matches).hasSize(4);

        long byesCount = round1Matches.stream()
                .filter(m -> m.getTeamA() != null && m.getTeamB() == null)
                .count();
        long realMatchesCount = round1Matches.stream()
                .filter(m -> m.getTeamA() != null && m.getTeamB() != null)
                .count();

        assertThat(byesCount).isEqualTo(3);
        assertThat(realMatchesCount).isEqualTo(1);

        round1Matches.stream()
                .filter(m -> m.getTeamB() == null)
                .forEach(m -> assertThat(m.getWinnerTeamId()).isEqualTo(m.getTeamA().getId()));
    }

    @Test
    @DisplayName("Powinien poprawnie podzielić 12 graczy na 6 drużyn w meczach 2v2 i nadać 2 wolne losy")
    void shouldCreateBracketFor12PlayersIn2v2() {
        // given
        List<Participant> participants = generateParticipants(12);
        int teamSize = 2;

        // when
        Tournament tournament = tournamentFactory.createTournament("Turniej Piłkarzyków", participants, teamSize);

        // then
        assertThat(tournament.getMatches()).hasSize(7);

        List<Match> round1Matches = getMatchesByRound(tournament, 1);

        long byesCount = round1Matches.stream()
                .filter(m -> m.getTeamA() != null && m.getTeamB() == null)
                .count();

        assertThat(byesCount).isEqualTo(2);

        Match sampleMatch = round1Matches.getFirst();
        assertThat(sampleMatch.getTeamA().getMembers()).hasSize(2);
    }

    @Test
    @DisplayName("Powinien wyrzucić wyjątek, gdy liczba graczy nie dzieli się równo na zespoły (K > 1)")
    void shouldThrowExceptionWhenPlayersCannotFormFullTeams() {
        // given
        List<Participant> participants = generateParticipants(5);
        int teamSize = 2;

        // when & then
        assertThatThrownBy(() -> tournamentFactory.createTournament("Turniej", participants, teamSize))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to create tournament");
    }

    @Test
    @DisplayName("Powinien wyrzucić wyjątek dla pustej listy uczestników")
    void shouldThrowExceptionWhenParticipantsListIsEmpty() {
        // given
        List<Participant> emptyParticipants = new ArrayList<>();

        // when & then
        assertThatThrownBy(() -> tournamentFactory.createTournament("Turniej", emptyParticipants, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Illegal number of participants");
    }

    @Test
    @DisplayName("Powinien wyrzucić wyjątek, jeśli po podziale powstanie tylko 1 drużyna")
    void shouldThrowExceptionWhenNotEnoughTeams() {
        // given
        List<Participant> participants = generateParticipants(2);
        int teamSize = 2;

        // when & then
        assertThatThrownBy(() -> tournamentFactory.createTournament("Turniej", participants, teamSize))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Too little teams, to create tournament (minimum 2 teams)");
    }

    @Test
    @DisplayName("Powinien wyrzucić wyjątek dla rozmiaru drużyny poniżej 1")
    void shouldThrowExceptionWhenTeamSizeIsZeroOrLess() {
        // given
        List<Participant> participants = generateParticipants(4);
        int teamSize = 0;

        // when & then
        assertThatThrownBy(() -> tournamentFactory.createTournament("Turniej", participants, teamSize))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Team size must be greater than zero");
    }

    @Test
    @DisplayName("Powinien powiązać mecze finałowe z półfinałami (weryfikacja wskaźnika nextMatchId)")
    void shouldProperlyLinkMatchesInTheTree() {
        // given
        List<Participant> participants = generateParticipants(4);

        // when
        Tournament tournament = tournamentFactory.createTournament("Turniej 4", participants, 1);

        // then
        List<Match> round1Matches = getMatchesByRound(tournament, 1);
        List<Match> round2Matches = getMatchesByRound(tournament, 2);

        assertThat(round1Matches).hasSize(2);
        assertThat(round2Matches).hasSize(1);

        Match finalMatch = round2Matches.getFirst();
        assertThat(finalMatch.getNextMatchId()).isNull();

        for (Match m : round1Matches) {
            assertThat(m.getNextMatchId()).isEqualTo(finalMatch.getId());
        }
    }

    // --- Metody Pomocnicze ---

    private List<Participant> generateParticipants(int count) {
        List<Participant> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(new Participant(UUID.randomUUID(), "Player_" + i));
        }
        return list;
    }

    private List<Match> getMatchesByRound(Tournament tournament, int round) {
        return tournament.getMatches().stream()
                .filter(m -> m.getRoundNumber() == round)
                .toList();
    }
}