package com.app.prod.tournament.api;

import com.app.prod.config.security.GlobalSecurityManager;
import com.app.prod.tournament.dto.TournamentDto;
import com.app.prod.tournament.dto.TournamentResponse;
import com.app.prod.tournament.dto.UpdateTournamentRequest;
import com.app.prod.tournament.models.TournamentStatus;
import com.app.prod.tournament.models.TournamentType;
import com.app.prod.tournament.services.TournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("tournament")
@RequiredArgsConstructor
public class TournamentRestApi {

    private final GlobalSecurityManager globalSecurityManager;
    private final TournamentService tournamentService;

    @PostMapping()
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<Void> createTournament(
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam Integer teamSize,
            @RequestParam TournamentType type
            ) {
        var user = globalSecurityManager.getCurrentUser();
        tournamentService.createTournament(name, description, teamSize, type, user);
        return ResponseEntity.ok(null);
    }

    @PutMapping("/add-participant")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<Void> addParticipant(@RequestParam UUID tournamentId, @RequestParam List<UUID> userIds) {
        tournamentService.addParticipant(tournamentId, userIds);
        return ResponseEntity.ok(null);
    }

    @PostMapping("/start")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<TournamentDto> start(@RequestParam UUID tournamentId) {
        var user = globalSecurityManager.getCurrentUser();
        TournamentDto dto = tournamentService.start(tournamentId, user);
        return ResponseEntity.ok(dto);
    }

    @GetMapping()
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<List<TournamentResponse>> getTournaments(@RequestParam(required = false) TournamentStatus status) {
        var user = globalSecurityManager.getCurrentUser();
        List<TournamentResponse> response = tournamentService.getAllTournaments(status, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{tournamentId}")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<TournamentDto> getTournament(@PathVariable UUID tournamentId) {
        var user = globalSecurityManager.getCurrentUser();
        TournamentDto tournament = tournamentService.getTournament(tournamentId, user);
        return ResponseEntity.ok(tournament);
    }

    @PutMapping("/{tournamentId}/update")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<Void> updateTournament(@PathVariable UUID tournamentId, @RequestBody UpdateTournamentRequest request) {
        var user = globalSecurityManager.getCurrentUser();
        tournamentService.updateTournament(tournamentId, request.matchId(), request.winnerTeamId(), user);
        return ResponseEntity.ok(null);
    }
}
