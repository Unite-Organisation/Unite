package com.app.prod.post.service;

import com.app.prod.post.dto.AnnouncementRequest;
import com.app.prod.post.dto.EventRequest;
import com.app.prod.post.mappers.AnnouncementMapper;
import com.app.prod.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class EventService {

    private final PostRepository postRepository;
    private final Clock clock;

    public void createEvent(EventRequest request, UUID userId) {
        //TODO: check if manager or user can post events for building or area

        var now = LocalDateTime.now(clock);
        var id = UUID.randomUUID();
        postRepository.insertOne(AnnouncementMapper.fromRequestToRecordEvent(request, userId, now, id));

        log.info("Created announcement with name: {}", request.name());
    }
}
