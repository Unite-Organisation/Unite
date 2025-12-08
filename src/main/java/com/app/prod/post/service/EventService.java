package com.app.prod.post.service;

import com.app.prod.area.service.AreaService;
import com.app.prod.post.dto.EventRequest;
import com.app.prod.post.mappers.AnnouncementMapper;
import com.app.prod.post.repository.PostRepository;
import com.app.prod.utils.shared.EntityCreatedResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.AppUserRecord;
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
    private final AreaService areaService;

    public EntityCreatedResponse createEvent(EventRequest request, AppUserRecord user) {
        //TODO: check if manager or user can post events for building or area
        UUID areaId = null;
        if (request.buildingId() == null){
            areaId = areaService.getUserArea(user);
        }

        var now = LocalDateTime.now(clock);
        var id = UUID.randomUUID();
        postRepository.insertOne(AnnouncementMapper.fromRequestToRecordEvent(request, user.getId(), now, id, areaId));

        log.info("Created announcement with name: {}", request.name());
        return new EntityCreatedResponse(id);
    }
}
