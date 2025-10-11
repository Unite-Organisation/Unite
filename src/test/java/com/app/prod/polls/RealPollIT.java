package com.app.prod.polls;

import com.app.prod.builders.*;
import com.app.prod.config.IntegrationTest;
import com.app.prod.exceptions.TestDataException;
import com.app.prod.polls.dto.PollRequest;
import com.app.prod.polls.repository.PollOptionRepository;
import com.app.prod.polls.repository.PollRepository;
import com.app.prod.polls.service.PollService;
import com.app.prod.user.enums.UserRole;
import com.app.prod.user.service.UserRoleService;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.PollOptionsRecord;
import org.jooq.sources.tables.records.UsersRecord;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;



@Slf4j
@SpringBootTest
public class RealPollIT extends IntegrationTest {

    @Autowired
    private UserPersistanceFactory userPersistanceFactory;
    @Autowired
    private BuildingPersistenceFactory buildingPersistenceFactory;
    @Autowired
    private AreaPersistenceFactory areaPersistenceFactory;
    @Autowired
    private PollService pollService;
    @Autowired
    private PollRepository pollRepository;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private PollOptionRepository pollOptionRepository;
    @Autowired
    private Clock clock;

    @Test
    @Disabled("Needs work with clock and time passage")
    void pollScenario(){
        var area = areaPersistenceFactory.getNewArea().withRandomValues().buildAndSave();
        var building = buildingPersistenceFactory.getNewBuilding().withRandomValues().areaId(area.getId()).buildAndSave();
        var mananger = userPersistanceFactory.getNewUser().withRandomValues().userRole(UserRole.MANAGER).buildAndSave();
        var voters = createVoters(100, building.getId());

        var pollOptions = List.of("A", "B");
        var frequencies   = List.of(50, 45);
        PollRequest request = createPollRequest(building.getId(), pollOptions);

        pollService.createPoll(request, mananger.getId());
        var pollId = pollRepository.findAll().getFirst().getId();

        var pollOptionsRecords = pollOptionRepository.findAll().stream().sorted(Comparator.comparing(PollOptionsRecord::getOptionText)).toList();
        Map<UUID, Integer> pollVotesFrequencyMap = IntStream.range(0, pollOptionsRecords.size())
                .boxed()
                .collect(Collectors.toMap(
                        i -> pollOptionsRecords.get(i).getId(),
                        i -> frequencies.get(i)
                ));

        vote(95, pollId, voters, pollVotesFrequencyMap);

        pollService.getPollResult(pollId);
    }

    private List<UsersRecord> createVoters(int count, UUID buildingId){
        List<UsersRecord> users = new ArrayList<>();
        for (int i = 0; i < count; i++){
            users.add(userPersistanceFactory.getNewUser().withRandomValues().buildingId(buildingId).build());
        }

        userPersistanceFactory.batchBuildAndSave(users);
        return users;
    }


    private void vote(int howMany, UUID pollId, List<UsersRecord> voters, Map<UUID, Integer> results){
        int voterIndex = 0;
        for(var entry : results.entrySet()){
            for(int i = 0; i < entry.getValue(); i++){
                if(voterIndex >= voters.size()){
                    throw new TestDataException("There are more votes than voters");
                }

                UsersRecord voter = voters.get(voterIndex++);
                if(!voter.getUserRole().equals(userRoleService.getUserRoleId(UserRole.RESIDENT))){
                    //skip managers and admins
                    continue;
                }

                pollService.vote(voter.getId(), pollId, entry.getKey());
            }
        }
    }

    private PollRequest createPollRequest(UUID buildingId, List<String> pollOptions){
        var startPoll = LocalDateTime.now(clock);
        var endPoll = startPoll.plusDays(1);

        return new PollRequest(
                "Poll1",
                "description",
                null,
                buildingId,
                true,
                startPoll,
                endPoll,
                pollOptions
        );
    }

}
