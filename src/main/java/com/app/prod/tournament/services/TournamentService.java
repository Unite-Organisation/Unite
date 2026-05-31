package com.app.prod.tournament.services;

import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.exceptions.exceptions.UnauthorizedDataAccessException;
import com.app.prod.tournament.TournamentMapper;
import com.app.prod.tournament.dto.TournamentDto;
import com.app.prod.tournament.dto.TournamentDtoMapper;
import com.app.prod.tournament.dto.TournamentResponse;
import com.app.prod.tournament.models.*;
import com.app.prod.tournament.repository.TournamentParticipantRepository;
import com.app.prod.tournament.repository.TournamentRepository;
import com.app.prod.user.repository.UserRepository;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.AppUserRecord;
import org.jooq.sources.tables.records.TournamentParticipantRecord;
import org.jooq.sources.tables.records.TournamentRecord;
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

        Tournament tournament = tournamentFactory.createTournament(tournamentRecord.getName(), tournamentRecord.getId(), participants, tournamentRecord.getTeamSize());
        tournamentMapper.saveTournament(tournament);

        return TournamentDtoMapper.toDto(tournament);
    }

    public List<TournamentResponse> getAllTournaments(TournamentStatus status, AppUserRecord user) {
        return tournamentRepository.findAllByBuilidng(user.getBuildingId(), status);

    }

    public TournamentDto getTournament(UUID tournamentId) {
        return tournamentMapper.fetchTournament(tournamentId)
                .map(TournamentDtoMapper::toDto)
                .orElseThrow(() -> new BadRequestException("Tournament not found"));
    }
}
