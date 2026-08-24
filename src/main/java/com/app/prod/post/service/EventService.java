package com.app.prod.post.service;

import com.app.prod.access.BuildingScope;
import com.app.prod.eventbus.EventBus;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.exceptions.exceptions.UnauthorizedDataAccessException;
import com.app.prod.post.dto.EventRequest;
import com.app.prod.post.dto.PostResponse;
import com.app.prod.post.mappers.AnnouncementMapper;
import com.app.prod.post.repository.PostRepository;
import com.app.prod.storage.ContextStoragePrefix;
import com.app.prod.storage.dto.StoredFile;
import com.app.prod.storage.file.FileService;
import com.app.prod.storage.handlers.RemoveFilesEvent;
import com.app.prod.utils.shared.EntityCreatedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.JSONB;
import org.jooq.sources.tables.records.PostRecord;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class EventService {

    private final PostRepository postRepository;
    private final FileService fileService;
    private final Clock clock;
    private final EventBus eventBus;
    private final PostService postService;

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

    public PostResponse updateEvent(BuildingScope scope, @Valid EventRequest request) {
        PostRecord post = postRepository.findById(request.id()).orElseThrow(() -> new BadRequestException(new AppError(Code.POST_NOT_FOUND)));

        if (!post.getCreatedBy().equals(scope.userId())) {
            throw new UnauthorizedDataAccessException(AppError.of(Code.NOT_OWN_RESOURCE));
        }

        post.setName(request.name());
        post.setContent(request.content());
        post.setRelatedDate(request.relatedDate());
        post.setStartDateTime(request.startDate());
        post.setEndDateTime(request.endDate());
        post.setLocationName(request.location());
        post.setOnlineUrl(request.onlineUrl());
        post.setMaxAttendees(request.maxAttendees());

        List<String> actualKeys = fileService.toStoredFiles(post.getAttachments()).stream().map(StoredFile::key).toList();
        List<String> requestedKeys = request.fileKeys();

        List<String> removedKeys = actualKeys.stream()
                .filter(actualKey -> !requestedKeys.contains(actualKey))
                .toList();

        JSONB attachments = fileService.confirmUploaded(ContextStoragePrefix.EVENT, scope.userId(), requestedKeys);
        post.setAttachments(attachments);
        postRepository.update(post);

        eventBus.publish(new RemoveFilesEvent(removedKeys));

        log.info("Updated event with id: {} in building: {}", request.id(), scope.buildingId());
        return postService.getPost(scope, request.id());
    }
}
