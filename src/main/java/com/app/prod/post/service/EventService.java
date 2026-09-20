package com.app.prod.post.service;

import com.app.prod.access.BuildingScope;
import com.app.prod.event.repository.EventRepository;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.exceptions.exceptions.DataAlreadyExistsException;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.post.dto.EventPostUpdateRequest;
import com.app.prod.post.dto.EventPublishRequest;
import com.app.prod.post.enums.PostType;
import com.app.prod.post.mappers.AnnouncementMapper;
import com.app.prod.post.repository.PostRepository;
import com.app.prod.storage.ContextStoragePrefix;
import com.app.prod.storage.file.FileService;
import com.app.prod.utils.shared.EntityCreatedResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.PostRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Events in the building feed. The event exists on its own (created through the public event endpoints),
 * publishing it adds a post that points at it.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class EventService {

    private final PostRepository postRepository;
    private final EventRepository eventRepository;
    private final FileService fileService;
    private final Clock clock;

    public EntityCreatedResponse publishEvent(EventPublishRequest request, BuildingScope scope) {
        VisibilityWindow.validate(request.visibleFrom(), request.visibleTo());

        UUID eventId = eventRepository.findIdBySlug(request.slug())
                .orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.EVENT_NOT_FOUND)));

        if (postRepository.existsForEvent(eventId)) {
            throw new DataAlreadyExistsException(AppError.of(Code.EVENT_ALREADY_PUBLISHED));
        }

        var attachments = fileService.confirmUploaded(ContextStoragePrefix.EVENT, scope.userId(), request.fileKeys());
        var now = LocalDateTime.now(clock);
        var id = UUID.randomUUID();

        try {
            postRepository.insertOne(AnnouncementMapper.fromRequestToRecordEvent(request, scope, now, id, eventId, attachments));
        } catch (RuntimeException exception) {
            fileService.discard(attachments);
            throw exception;
        }

        log.info("Published event {} in building: {}", eventId, scope.buildingId());
        return new EntityCreatedResponse(id);
    }

    @Transactional
    public void updateEventPost(UUID id, EventPostUpdateRequest request, BuildingScope scope) {
        VisibilityWindow.validate(request.visibleFrom(), request.visibleTo());

        PostRecord post = postRepository.findInBuildingForUpdate(scope.buildingId(), id)
                .orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.POST_NOT_FOUND)));

        if (post.getEventId() == null) {
            throw new BadRequestException(AppError.of(Code.VALIDATION_ERROR, String.format("Post %s is not an %s", id, PostType.EVENT)));
        }

        post.setVisibleFrom(request.visibleFrom());
        post.setVisibleTo(request.visibleTo());

        //TODO: files update not supported yet
        postRepository.update(post);
        log.info("Updated event post: {} in building: {}", id, scope.buildingId());
    }

}
