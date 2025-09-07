package com.app.prod.polls.service;

import com.app.prod.polls.dto.PollRequest;
import com.app.prod.polls.dto.PollResponse;
import com.app.prod.polls.mappers.PollMapper;
import com.app.prod.polls.repository.PollOptionRepository;
import com.app.prod.polls.repository.PollRepository;
import com.app.prod.utils.Pagination;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.PollOptionsRecord;
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

    @Transactional
    public void createPoll(PollRequest request, UUID userId) {
        var now = LocalDateTime.now(clock);
        var pollId = UUID.randomUUID();

        pollRepository.insertOne(PollMapper.fromRequestToRecord(request, userId, now, pollId));

        List<PollOptionsRecord> records = request.options().stream()
                .map(option -> new PollOptionsRecord(
                        UUID.randomUUID(),
                        pollId,
                        option
                )
        ).toList();

        pollOptionRepository.insertMany(records);

        log.info("User {} created poll: {}", userId, request.title());
    }

    public List<PollResponse> getPolls(UUID userId, Pagination pagination) {
        return pollRepository.getPolls(userId, pagination);
    }
}
