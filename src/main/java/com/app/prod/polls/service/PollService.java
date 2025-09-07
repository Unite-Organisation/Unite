package com.app.prod.polls.service;

import com.app.prod.polls.dto.PollRequest;
import com.app.prod.polls.dto.PollResponse;
import com.app.prod.polls.mappers.PollMapper;
import com.app.prod.polls.repository.PollOptionRepository;
import com.app.prod.polls.repository.PollRepository;
import com.app.prod.polls.repository.PollResultRepository;
import com.app.prod.polls.repository.PollVotesRepository;
import com.app.prod.utils.Pagination;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.PollResult;
import org.jooq.sources.tables.records.PollOptionsRecord;
import org.jooq.sources.tables.records.PollResultRecord;
import org.jooq.sources.tables.records.PollVotesRecord;
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

    @Transactional
    public void createPoll(PollRequest request, UUID userId) {
        var now = LocalDateTime.now(clock);
        var pollId = UUID.randomUUID();

        createPollData(request, userId, now, pollId);
        log.info("User {} created poll: {}", userId, request.title());
    }

    private void createPollData(PollRequest request, UUID userId, LocalDateTime now, UUID pollId) {
        pollRepository.insertOne(PollMapper.fromRequestToRecord(request, userId, now, pollId));

        List<PollOptionsRecord> records = request.options().stream()
                .map(option -> new PollOptionsRecord(
                        UUID.randomUUID(),
                        pollId,
                        option
                )
        ).toList();

        pollOptionRepository.insertMany(records);

        pollResultRepository.insertOne(new PollResultRecord(
            UUID.randomUUID(),
                pollId,
            null,
            null,
            false
        ));
    }

    public List<PollResponse> getPolls(UUID userId, Pagination pagination) {
        return pollRepository.getPolls(userId, pagination);
    }

    public void vote(UUID userId, UUID pollId, UUID vote) {
        validate.thatUserCanVote(userId, pollId);

        var now = LocalDateTime.now(clock);

        pollVotesRepository.insertOne(new PollVotesRecord(
                UUID.randomUUID(),
                pollId,
                vote,
                userId,
                now
        ));

        log.info("User {} voted for {} at {}", userId, vote, now);
    }
}
