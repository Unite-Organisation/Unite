package com.app.prod.simulation;

import com.app.prod.tournament.repository.TournamentParticipantRepository;
import com.app.prod.tournament.repository.TournamentRepository;
import com.app.prod.user.enums.UserRole;
import com.app.prod.user.enums.UserStatus;
import com.app.prod.user.repository.UserRepository;
import com.app.prod.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.TournamentParticipant;
import org.jooq.sources.tables.records.AppUserRecord;
import org.jooq.sources.tables.records.TournamentParticipantRecord;
import org.jooq.sources.tables.records.TournamentRecord;
import org.jooq.sources.tables.records.UserRoleRecord;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RequestMapping("simulation")
@RestController
public class SimulationRestApi {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final TournamentParticipantRepository tournamentParticipantRepository;
    private final TournamentRepository tournamentRepository;

    @PostMapping("/tournament-participants")
    @Transactional
    public void createFunnyUsers(@RequestParam UUID tournamentId, @RequestParam Integer count) {
        TournamentRecord tournamentRecord = tournamentRepository.findById(tournamentId).orElseThrow();
        UUID residentRole = userRoleRepository.findByRoleName(UserRole.RESIDENT).map(UserRoleRecord::getId).orElseThrow();

        List<AppUserRecord> users = new ArrayList<>();
        List<TournamentParticipantRecord> participants = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            UUID userId = UUID.randomUUID();

            AppUserRecord user = new AppUserRecord();
            user.setId(userId);
            user.setFirstName("FUNNY-" + i);
            user.setLastName("USER-" + i);
            user.setEmail(UUID.randomUUID().toString().substring(0, 16));
            user.setUsername(UUID.randomUUID().toString().substring(0, 8) + "-FUNNY-" + i);
            user.setPassword(UUID.randomUUID().toString());
            user.setUserRole(residentRole);
            user.setStatus(UserStatus.ACTIVE.name());
            user.setBuildingId(tournamentRecord.getBuildingId());

            participants.add(new TournamentParticipantRecord(UUID.randomUUID(), tournamentId, userId));
            users.add(user);
        }

        userRepository.insertMany(users);
        tournamentParticipantRepository.insertMany(participants);
    }

}
