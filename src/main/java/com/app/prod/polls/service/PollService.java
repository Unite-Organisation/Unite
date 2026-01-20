package com.app.prod.polls.service;

import com.app.prod.area.service.AreaService;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.polls.dto.PollRequest;
import com.app.prod.polls.dto.PollResponse;
import com.app.prod.polls.dto.PollResult;
import com.app.prod.polls.mappers.PollMapper;
import com.app.prod.polls.repository.PollOptionRepository;
import com.app.prod.polls.repository.PollRepository;
import com.app.prod.polls.repository.PollResultRepository;
import com.app.prod.polls.repository.PollVotesRepository;
import com.app.prod.utils.Pagination;
import com.app.prod.utils.filters.PollFilter;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.Poll;
import org.jooq.sources.tables.records.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PollService {

    private final Clock clock;
    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollVotesRepository pollVotesRepository;
    private final Validate validate;
    private final PollResultRepository pollResultRepository;
    private final PollResultService pollResultService;
    private final AreaService areaService;

    @Transactional
    public void createPoll(PollRequest request, AppUserRecord user) {
        //TODO: check if manager can create polls for this building or area
        UUID areaId = null;
        if (request.buildingId() == null){
            areaId = areaService.getUserArea(user);
        }

        var now = LocalDateTime.now(clock);
        var pollId = UUID.randomUUID();

        createPollData(request, user.getId(), now, pollId, areaId);
        log.info("User {} created poll: {}", user.getId(), request.title());
    }

    private void createPollData(PollRequest request, UUID userId, LocalDateTime now, UUID pollId, UUID areaId) {
        pollRepository.insertOne(PollMapper.fromRequestToRecord(request, userId, now, pollId, areaId));

        List<PollOptionRecord> records = request.options().stream()
                .map(option -> new PollOptionRecord(
                        UUID.randomUUID(),
                        pollId,
                        option,
                        0
                )
        ).toList();

        pollOptionRepository.insertMany(records);

        pollResultRepository.insertOne(new PollResultRecord(
            UUID.randomUUID(),
                pollId,
            null,
            false
        ));
    }

    public List<PollResponse> getPolls(UUID userId, Pagination pagination, PollFilter pollFilter) {
        return pollRepository.getPolls(userId, pagination, pollFilter);
    }

    @Transactional
    public void vote(AppUserRecord user, UUID pollId, UUID vote) {
        validate.thatUserCanVote(user, pollId);

        var now = LocalDateTime.now(clock);

        pollVotesRepository.insertOne(new PollVoteRecord(
                UUID.randomUUID(),
                pollId,
                vote,
                user.getId(),
                now
        ));

        pollOptionRepository.addVoteForOption(vote);

        log.info("User {} voted for {} at {}", user.getId(), vote, now);
    }

    public PollResult getPollResult(UUID pollId) {
        validate.poll(pollId);
        log.info("Starting processing poll {} result", pollId);
        return pollResultService.calculatePollResult(pollId);
    }


    public PollRecord findById(UUID pollId){
        return pollRepository.findById(pollId).orElseThrow(
                () -> new EntityNotPresentException(
                        String.format("Poll with id: %s does not exist", pollId),
                        Poll.class.getSimpleName()
                )
        );
    }
}
