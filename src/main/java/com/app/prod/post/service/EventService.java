package com.app.prod.post.service;

import com.app.prod.access.BuildingScope;
import com.app.prod.post.dto.EventRequest;
import com.app.prod.post.mappers.AnnouncementMapper;
import com.app.prod.post.repository.PostRepository;
import com.app.prod.storage.ContextStoragePrefix;
import com.app.prod.storage.file.FileService;
import com.app.prod.utils.shared.EntityCreatedResponse;
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
    private final FileService fileService;
    private final Clock clock;

    public EntityCreatedResponse createEvent(EventRequest request, BuildingScope scope) {
        var attachments = fileService.confirmUploaded(ContextStoragePrefix.EVENT, scope.userId(), request.fileKeys());
        var now = LocalDateTime.now(clock);
        var id = UUID.randomUUID();

        try {
            postRepository.insertOne(AnnouncementMapper.fromRequestToRecordEvent(request, scope, now, id, attachments));
        } catch (RuntimeException exception) {
            fileService.discard(attachments);
            throw exception;
        }

        log.info("Created event with name: {} in building: {}", request.name(), scope.buildingId());
        return new EntityCreatedResponse(id);
    }
}
