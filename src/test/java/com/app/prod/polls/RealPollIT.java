package com.app.prod.polls;

import com.app.prod.builders.*;
import com.app.prod.config.IntegrationTest;
import com.app.prod.config.MutableClock;
import com.app.prod.exceptions.TestDataException;
import com.app.prod.polls.dto.PollOptionPercentageShare;
import com.app.prod.polls.dto.PollOptionVoteCount;
import com.app.prod.polls.dto.PollRequest;
import com.app.prod.polls.dto.PollResult;
import com.app.prod.polls.repository.PollOptionRepository;
import com.app.prod.polls.repository.PollRepository;
import com.app.prod.polls.service.PollService;
import com.app.prod.services.schedulers.PollScheduler;
import com.app.prod.user.enums.UserRole;
import com.app.prod.user.service.UserRoleService;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.AreaRecord;
import org.jooq.sources.tables.records.BuildingRecord;
import org.jooq.sources.tables.records.PollOptionRecord;
import org.jooq.sources.tables.records.AppUserRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;


@Slf4j
@SpringBootTest
@ActiveProfiles("test")
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
    private MutableClock clock;
    @Autowired
    private PollScheduler pollScheduler;

    private AreaRecord area;
    private BuildingRecord building;
    private AppUserRecord manager;

    // IMPORTANT: poll options should only be characters 'A' 'B' and so on. They will be sorted.

    @BeforeEach
    void setUp(){
        area = areaPersistenceFactory.getNewArea().withRandomValues().buildAndSave();
        building = buildingPersistenceFactory.getNewBuilding().withRandomValues().areaId(area.getId()).buildAndSave();
        manager = userPersistanceFactory.getNewUser().withRandomValues().userRole(UserRole.MANAGER).buildAndSave();
    }

    @Test
    void pollScenarioTwoOptions(){
        var voters = createVoters(100, building.getId());
        var pollOptions = List.of("A", "B");
        var frequencies   = List.of(50, 45);

        createPoll(pollOptions, "Poll-1");

        var pollId = pollRepository.findAll().stream().filter(r -> r.getTitle().equals("Poll-1")).toList().getFirst().getId();
        var pollOptionsRecords = fetchSortedPollOptions(pollId);

        vote(95, pollId, voters, pollOptionsRecords, frequencies);

        clock.advance(Duration.ofDays(3));
        pollScheduler.finishPoll();
        PollResult result = pollService.getPollResult(pollId);

        assertThat(result.winners().getFirst().optionId()).isEqualTo(pollOptionsRecords.getFirst().getId());
        assertThat(result.numberOfVotes()).isEqualTo(95);
        assertThat(result.numberOfPeopleEligibleToVote()).isEqualTo(100);
        assertThat(result.votersPercentage()).isEqualByComparingTo(BigDecimal.valueOf(0.95));

        assertThat(result.sortedVotes().getFirst().optionId()).isEqualTo(pollOptionsRecords.getFirst().getId());
        assertThat(result.sortedVotes().getFirst().count()).isEqualTo(50);
        assertThat(result.sortedVotes().get(1).optionId()).isEqualTo(pollOptionsRecords.get(1).getId());
        assertThat(result.sortedVotes().get(1).count()).isEqualTo(45);

        assertThat(result.sortedPercentageShareOfVotes().getFirst().optionId()).isEqualTo(pollOptionsRecords.getFirst().getId());
        assertThat(result.sortedPercentageShareOfVotes().getFirst().percentageShare()).isEqualByComparingTo(BigDecimal.valueOf(0.53));
        assertThat(result.sortedPercentageShareOfVotes().get(1).optionId()).isEqualTo(pollOptionsRecords.get(1).getId());
        assertThat(result.sortedPercentageShareOfVotes().get(1).percentageShare()).isEqualByComparingTo(BigDecimal.valueOf(0.47));
    }

    @Test
    void pollScenarioFiveOptions(){
        var voters = createVoters(43, building.getId());
        var pollOptions = List.of("A", "B", "C", "D", "E");
        var frequencies   = List.of(33, 7, 2, 1, 0);

        createPoll(pollOptions, "Poll-2");

        var pollId = pollRepository.findAll().stream().filter(r -> r.getTitle().equals("Poll-2")).toList().getFirst().getId();
        var pollOptionsRecords = fetchSortedPollOptions(pollId);

        vote(43, pollId, voters, pollOptionsRecords, frequencies);

        clock.advance(Duration.ofDays(3));
        pollScheduler.finishPoll();
        PollResult result = pollService.getPollResult(pollId);

        assertThat(result.winners().getFirst().optionId()).isEqualTo(pollOptionsRecords.getFirst().getId());
        assertThat(result.numberOfVotes()).isEqualTo(43);
        assertThat(result.numberOfPeopleEligibleToVote()).isEqualTo(43);
        assertThat(result.votersPercentage()).isEqualByComparingTo(BigDecimal.valueOf(1));

        assertThat(result.sortedVotes()).hasSize(5);
        assertThat(result.sortedPercentageShareOfVotes()).hasSize(5);


        assertThat(result.sortedVotes().get(0).optionId()).isEqualTo(pollOptionsRecords.get(0).getId());
        assertThat(result.sortedVotes().get(0).count()).isEqualTo(33);
        assertThat(result.sortedVotes().get(1).optionId()).isEqualTo(pollOptionsRecords.get(1).getId());
        assertThat(result.sortedVotes().get(1).count()).isEqualTo(7);
        assertThat(result.sortedVotes().get(2).optionId()).isEqualTo(pollOptionsRecords.get(2).getId());
        assertThat(result.sortedVotes().get(2).count()).isEqualTo(2);
        assertThat(result.sortedVotes().get(3).optionId()).isEqualTo(pollOptionsRecords.get(3).getId());
        assertThat(result.sortedVotes().get(3).count()).isEqualTo(1);
        assertThat(result.sortedVotes().get(4).optionId()).isEqualTo(pollOptionsRecords.get(4).getId());
        assertThat(result.sortedVotes().get(4).count()).isEqualTo(0);

        assertThat(result.sortedPercentageShareOfVotes().get(0).optionId()).isEqualTo(pollOptionsRecords.get(0).getId());
        assertThat(result.sortedPercentageShareOfVotes().get(0).percentageShare()).isEqualByComparingTo(BigDecimal.valueOf(0.77));
        assertThat(result.sortedPercentageShareOfVotes().get(1).optionId()).isEqualTo(pollOptionsRecords.get(1).getId());
        assertThat(result.sortedPercentageShareOfVotes().get(1).percentageShare()).isEqualByComparingTo(BigDecimal.valueOf(0.16));
        assertThat(result.sortedPercentageShareOfVotes().get(2).optionId()).isEqualTo(pollOptionsRecords.get(2).getId());
        assertThat(result.sortedPercentageShareOfVotes().get(2).percentageShare()).isEqualByComparingTo(BigDecimal.valueOf(0.05));
        assertThat(result.sortedPercentageShareOfVotes().get(3).optionId()).isEqualTo(pollOptionsRecords.get(3).getId());
        assertThat(result.sortedPercentageShareOfVotes().get(3).percentageShare()).isEqualByComparingTo(BigDecimal.valueOf(0.02));
        assertThat(result.sortedPercentageShareOfVotes().get(4).optionId()).isEqualTo(pollOptionsRecords.get(4).getId());
        assertThat(result.sortedPercentageShareOfVotes().get(4).percentageShare()).isEqualByComparingTo(BigDecimal.valueOf(0.0));
    }

    @Test
    void pollScenarioManyPeopleDidntVote(){
        var voters = createVoters(14, building.getId());
        var pollOptions = List.of("A", "B", "C");
        var frequencies   = List.of(6, 0, 0);

        createPoll(pollOptions, "Poll-3");

        var pollId = pollRepository.findAll().stream().filter(r -> r.getTitle().equals("Poll-3")).toList().getFirst().getId();
        var pollOptionsRecords = fetchSortedPollOptions(pollId);

        vote(6, pollId, voters, pollOptionsRecords, frequencies);

        clock.advance(Duration.ofDays(3));
        pollScheduler.finishPoll();
        PollResult result = pollService.getPollResult(pollId);

        assertThat(result.winners().getFirst().optionId()).isEqualTo(pollOptionsRecords.getFirst().getId());
        assertThat(result.numberOfVotes()).isEqualTo(6);
        assertThat(result.numberOfPeopleEligibleToVote()).isEqualTo(14);
        assertThat(result.votersPercentage()).isEqualByComparingTo(BigDecimal.valueOf(0.43));

        assertThat(result.sortedVotes()).hasSize(3);
        assertThat(result.sortedPercentageShareOfVotes()).hasSize(3);

        assertThat(result.sortedVotes().get(0).optionId()).isEqualTo(pollOptionsRecords.get(0).getId());
        assertThat(result.sortedVotes().get(0).count()).isEqualTo(6);
        assertThat(result.sortedVotes().get(1).optionId()).isEqualTo(pollOptionsRecords.get(1).getId());
        assertThat(result.sortedVotes().get(1).count()).isEqualTo(0);
        assertThat(result.sortedVotes().get(2).optionId()).isEqualTo(pollOptionsRecords.get(2).getId());
        assertThat(result.sortedVotes().get(2).count()).isEqualTo(0);

        assertThat(result.sortedPercentageShareOfVotes().get(0).optionId()).isEqualTo(pollOptionsRecords.get(0).getId());
        assertThat(result.sortedPercentageShareOfVotes().get(0).percentageShare()).isEqualByComparingTo(BigDecimal.valueOf(1));
        assertThat(result.sortedPercentageShareOfVotes().get(1).optionId()).isEqualTo(pollOptionsRecords.get(1).getId());
        assertThat(result.sortedPercentageShareOfVotes().get(1).percentageShare()).isEqualByComparingTo(BigDecimal.valueOf(0));
        assertThat(result.sortedPercentageShareOfVotes().get(2).optionId()).isEqualTo(pollOptionsRecords.get(2).getId());
        assertThat(result.sortedPercentageShareOfVotes().get(2).percentageShare()).isEqualByComparingTo(BigDecimal.valueOf(0));
    }

    @Test
    void pollScenarioNoOneVoted(){
        var voters = createVoters(20, building.getId());
        var pollOptions = List.of("A", "B");

        createPoll(pollOptions, "Poll-4");

        var pollId = pollRepository.findAll().stream().filter(r -> r.getTitle().equals("Poll-4")).toList().getFirst().getId();
        var pollOptionsRecords = fetchSortedPollOptions(pollId);

        clock.advance(Duration.ofDays(3));
        pollScheduler.finishPoll();
        PollResult result = pollService.getPollResult(pollId);

        assertThat(result.winners()).isEmpty();
        assertThat(result.numberOfVotes()).isEqualTo(0);
        assertThat(result.numberOfPeopleEligibleToVote()).isEqualTo(20);
        assertThat(result.votersPercentage()).isEqualByComparingTo(BigDecimal.valueOf(0));

        assertThat(result.sortedVotes()).hasSize(2);
        assertThat(result.sortedPercentageShareOfVotes()).hasSize(2);

        assertThat(result.sortedVotes().get(0).optionId()).isEqualTo(pollOptionsRecords.get(0).getId());
        assertThat(result.sortedVotes().get(0).count()).isEqualTo(0);
        assertThat(result.sortedVotes().get(1).optionId()).isEqualTo(pollOptionsRecords.get(1).getId());
        assertThat(result.sortedVotes().get(1).count()).isEqualTo(0);

        assertThat(result.sortedPercentageShareOfVotes().get(0).optionId()).isEqualTo(pollOptionsRecords.get(0).getId());
        assertThat(result.sortedPercentageShareOfVotes().get(0).percentageShare()).isEqualByComparingTo(BigDecimal.valueOf(0));
        assertThat(result.sortedPercentageShareOfVotes().get(1).optionId()).isEqualTo(pollOptionsRecords.get(1).getId());
        assertThat(result.sortedPercentageShareOfVotes().get(1).percentageShare()).isEqualByComparingTo(BigDecimal.valueOf(0));
    }

    @Test
    void pollScenarioManyWinners(){
        var voters = createVoters(10, building.getId());
        var pollOptions = List.of("A", "B", "C");
        var frequencies   = List.of(3, 3, 1);

        createPoll(pollOptions, "Poll-5");

        var pollId = pollRepository.findAll().stream().filter(r -> r.getTitle().equals("Poll-5")).toList().getFirst().getId();
        var pollOptionsRecords = fetchSortedPollOptions(pollId);

        vote(7, pollId, voters, pollOptionsRecords, frequencies);

        clock.advance(Duration.ofDays(3));
        pollScheduler.finishPoll();
        PollResult result = pollService.getPollResult(pollId);

        assertThat(result.winners()).hasSize(2);
        assertThat(result.winners()).extracting(PollOptionVoteCount::optionId)
                .containsExactlyInAnyOrder(pollOptionsRecords.get(0).getId(), pollOptionsRecords.get(1).getId());
        assertThat(result.numberOfVotes()).isEqualTo(7);
        assertThat(result.numberOfPeopleEligibleToVote()).isEqualTo(10);
        assertThat(result.votersPercentage()).isEqualByComparingTo(BigDecimal.valueOf(0.7));

        assertThat(result.sortedVotes()).hasSize(3);
        assertThat(result.sortedPercentageShareOfVotes()).hasSize(3);

        // Verify the two winners (A and B) both have 3 votes - order is not deterministic when tied
        assertThat(result.sortedVotes().get(0).count()).isEqualTo(3);
        assertThat(result.sortedVotes().get(1).count()).isEqualTo(3);
        assertThat(result.sortedVotes().subList(0, 2)).extracting(PollOptionVoteCount::optionId)
                .containsExactlyInAnyOrder(pollOptionsRecords.get(0).getId(), pollOptionsRecords.get(1).getId());

        // Verify the loser (C) has 1 vote
        assertThat(result.sortedVotes().get(2).optionId()).isEqualTo(pollOptionsRecords.get(2).getId());
        assertThat(result.sortedVotes().get(2).count()).isEqualTo(1);

        // Verify percentage shares - the two winners should both have 0.43 (43%)
        assertThat(result.sortedPercentageShareOfVotes().get(0).percentageShare()).isEqualByComparingTo(BigDecimal.valueOf(0.43));
        assertThat(result.sortedPercentageShareOfVotes().get(1).percentageShare()).isEqualByComparingTo(BigDecimal.valueOf(0.43));
        assertThat(result.sortedPercentageShareOfVotes().subList(0, 2)).extracting(PollOptionPercentageShare::optionId)
                .containsExactlyInAnyOrder(pollOptionsRecords.get(0).getId(), pollOptionsRecords.get(1).getId());

        // Verify the loser has 0.14 (14%)
        assertThat(result.sortedPercentageShareOfVotes().get(2).optionId()).isEqualTo(pollOptionsRecords.get(2).getId());
        assertThat(result.sortedPercentageShareOfVotes().get(2).percentageShare()).isEqualByComparingTo(BigDecimal.valueOf(0.14));
    }


    private void createPoll(List<String> pollOptions, String pollName){
        PollRequest request = createPollRequest(building.getId(), pollOptions, pollName);
        pollService.createPoll(request, manager);
    }

    private List<PollOptionRecord> fetchSortedPollOptions(UUID pollId){
        return pollOptionRepository.findAll().stream()
                .filter(r -> r.getPollId().equals(pollId))
                .sorted(Comparator.comparing(PollOptionRecord::getOptionText))
                .toList();
    }

    private List<AppUserRecord> createVoters(int count, UUID buildingId){
        List<AppUserRecord> users = new ArrayList<>();
        for (int i = 0; i < count; i++){
            users.add(userPersistanceFactory.getNewUser().withRandomValues().buildingId(buildingId).build());
        }

        userPersistanceFactory.batchBuildAndSave(users);
        return users;
    }

    private void vote(int howMany, UUID pollId, List<AppUserRecord> voters, List<PollOptionRecord> pollOptionsRecords, List<Integer> frequencies){

        Map<UUID, Integer> results = IntStream.range(0, pollOptionsRecords.size())
                .boxed()
                .collect(Collectors.toMap(
                        i -> pollOptionsRecords.get(i).getId(),
                        i -> frequencies.get(i)
                ));

        int voterIndex = 0;
        for(var entry : results.entrySet()){
            for(int i = 0; i < entry.getValue(); i++){
                if(voterIndex >= voters.size()){
                    throw new TestDataException("There are more votes than voters");
                }

                AppUserRecord voter = voters.get(voterIndex++);
                if(!voter.getUserRole().equals(userRoleService.getUserRoleId(UserRole.RESIDENT))){
                    //skip managers and admins
                    continue;
                }

                pollService.vote(voter.getId(), pollId, entry.getKey());
            }
        }
    }

    private PollRequest createPollRequest(UUID buildingId, List<String> pollOptions, String pollName){
        var startPoll = LocalDateTime.now(clock);
        var endPoll = startPoll.plusDays(1);

        return new PollRequest(
                pollName,
                "description",
                buildingId,
                true,
                startPoll,
                endPoll,
                pollOptions
        );
    }

}
