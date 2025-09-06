package com.app.prod.polls.service;

import com.app.prod.polls.dto.PollRequest;
import com.app.prod.polls.mappers.PollMapper;
import com.app.prod.polls.repository.PollRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PollService {

    private final Clock clock;
    private final PollRepository pollRepository;

    public void createPoll(PollRequest request, UUID userId) {
        var now = LocalDateTime.now(clock);
        pollRepository.insertOne(PollMapper.fromRequestToRecord(request, userId, now));
        log.info("User {} created poll: {}", userId, request.title());
    }
}
