package com.app.prod.tournament.services;

import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.exceptions.exceptions.UnauthorizedDataAccessException;
import com.app.prod.tournament.TournamentMapper;
import com.app.prod.tournament.dto.TournamentDto;
import com.app.prod.tournament.dto.TournamentDtoMapper;
import com.app.prod.tournament.dto.TournamentResponse;
import com.app.prod.tournament.models.*;
import com.app.prod.tournament.repository.TournamentMatchRepository;
import com.app.prod.tournament.repository.TournamentParticipantRepository;
import com.app.prod.tournament.repository.TournamentRepository;
import com.app.prod.user.repository.UserRepository;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.AppUserRecord;
import org.jooq.sources.tables.records.TournamentMatchRecord;
import org.jooq.sources.tables.records.TournamentParticipantRecord;
import org.jooq.sources.tables.records.TournamentRecord;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final TournamentParticipantRepository tournamentParticipantRepository;
    private final TournamentMatchRepository tournamentMatchRepository;
    private final UserRepository userRepository;
    private final TournamentFactory tournamentFactory;
    private final TournamentMapper tournamentMapper;
    private final Validate validate;

    public void createTournament(String name, String descritpion, Integer teamSize, TournamentType tournamentType, AppUserRecord user) {
        var buildingId = user.getBuildingId();

        if (buildingId == null) {
            log.warn("Creating tournament only available for users right now");
            throw new BadRequestException("Creating tournament only available for users right now");
        }

        TournamentRecord tournament = new TournamentRecord();
        tournament.setBuildingId(buildingId);
        tournament.setName(name);
        tournament.setDescription(descritpion);
        tournament.setCreatedBy(user.getId());
        tournament.setTeamSize(teamSize);
        tournament.setType(tournamentType.name());

        tournamentRepository.insertOne(tournament);
    }

    public void addParticipant(UUID tournamentId, List<UUID> users) {
        //TODO: check privileges and so on

        List<TournamentParticipantRecord> records = new ArrayList<>();
        for (var userId : users) {
            validate.user(userId); //TODO: one query before loop

            TournamentParticipantRecord record = new TournamentParticipantRecord();
            record.setUserId(userId);
            record.setTournamentId(tournamentId);

            records.add(record);
        }

        tournamentParticipantRepository.insertMany(records);
    }

    @Transactional
    public TournamentDto start(UUID tournamentId, AppUserRecord user) {
        TournamentRecord tournamentRecord = tournamentRepository.findById(tournamentId).orElseThrow(() -> new BadRequestException("Tournament not exists"));

        if (!tournamentRecord.getCreatedBy().equals(user.getId())) {
            throw new UnauthorizedDataAccessException("User is not tournament creator");
        }

        List<UUID> participantsUUIDs = tournamentParticipantRepository.findByTournament(tournamentId).stream()
                .map(TournamentParticipantRecord::getUserId).toList();

        List<AppUserRecord> usersParticipants = userRepository.findByIds(participantsUUIDs);

        List<Participant> participants = usersParticipants.stream()
                .map(up -> new Participant(up.getId(), up.getUsername())).toList();

        Tournament tournament = tournamentFactory.createTournament(tournamentRecord.getName(), tournamentRecord.getId(), participants, tournamentRecord.getTeamSize(), tournamentRecord.getCreatedBy());
        tournamentMapper.saveTournament(tournament);

        return TournamentDtoMapper.toDto(tournament, user);
    }

    public List<TournamentResponse> getAllTournaments(TournamentStatus status, AppUserRecord user) {
        return tournamentRepository.findAllByBuilidng(user.getBuildingId(), status, user.getId());

    }

    private Tournament getTournamentRaw(UUID tournamentId) {
        return tournamentMapper.fetchTournament(tournamentId)
                .orElseThrow(() -> new BadRequestException("Tournament not found"));
    }

    public TournamentDto getTournament(UUID tournamentId, AppUserRecord user) {
        return TournamentDtoMapper.toDto(getTournamentRaw(tournamentId), user);
    }

    @Transactional
    public void updateTournament(UUID tournamentId, UUID matchId, UUID winnerTeamId, AppUserRecord user) {
        TournamentRecord tournamentRecord = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new BadRequestException("Tournament not found"));

        if (!tournamentRecord.getCreatedBy().equals(user.getId())) {
            throw new UnauthorizedDataAccessException("User is not tournament creator");
        }

        TournamentMatchRecord matchRecord = tournamentMatchRepository.findById(matchId)
                .orElseThrow(() -> new BadRequestException("Match not found"));

        if (matchRecord.getTeamAId().equals(winnerTeamId) || matchRecord.getTeamBId().equals(winnerTeamId)) {
            matchRecord.setWinnerTeamId(winnerTeamId);
        } else {
            throw new BadRequestException("Provided winner team is not part of this match");
        }

        matchRecord.setStatus(MatchStatus.OVER.name());
        tournamentMatchRepository.update(matchRecord);

        //it was last match (no more work to do)
        if (matchRecord.getNextMatchId() == null) {
            tournamentRecord.setStatus(TournamentStatus.FINISHED.name());
            tournamentRepository.update(tournamentRecord);
            return;
        }

        TournamentMatchRecord nextMatchRecord = tournamentMatchRepository.findById(matchRecord.getNextMatchId())
                .orElseThrow(() -> new BadRequestException("NextMatch not found"));

        if (nextMatchRecord.getTeamAId() == null) {
            nextMatchRecord.setTeamAId(winnerTeamId);
        } else if (nextMatchRecord.getTeamBId() == null) {
            nextMatchRecord.setTeamBId(winnerTeamId);
        }

        tournamentMatchRepository.update(nextMatchRecord);
    }
}
