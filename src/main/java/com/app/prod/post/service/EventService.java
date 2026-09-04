package com.app.prod.post.service;

import com.app.prod.access.BuildingScope;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.interaction.enums.InteractionEntityType;
import com.app.prod.interaction.enums.InteractionType;
import com.app.prod.interaction.repository.InteractionRepository;
import com.app.prod.post.dto.EventRequest;
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
import java.util.Objects;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class EventService {

    private final PostRepository postRepository;
    private final InteractionRepository interactionRepository;
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

    @Transactional
    public void updateEvent(UUID id, EventRequest request, BuildingScope scope) {
        assertEvent(request.postType());

        PostRecord event = postRepository.findInBuildingForUpdate(scope.buildingId(), id)
                .orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.POST_NOT_FOUND)));

        event.setName(request.name());
        event.setContent(request.content());
        event.setRelatedDate(request.relatedDate());
        event.setStartDateTime(request.startDate());
        event.setEndDateTime(request.endDate());
        event.setLocationName(request.location());
        event.setOnlineUrl(request.onlineUrl());

        if (!Objects.equals(event.getMaxAttendees(), request.maxAttendees())) {
            assertLimitFitsAttendees(id, request.maxAttendees());
            event.setMaxAttendees(request.maxAttendees());
        }

        //TODO: files update not supported yet
        postRepository.update(event);
        log.info("Updated event: {} in building: {}", id, scope.buildingId());
    }

    private void assertLimitFitsAttendees(UUID eventId, Integer maxAttendees) {
        if (maxAttendees == null) {
            return;
        }

        int attendeesCount = interactionRepository.countInteractions(InteractionEntityType.POST, eventId, InteractionType.ATTENDING);
        if (attendeesCount > maxAttendees) {
            throw new BadRequestException(AppError.of(
                    Code.EVENT_MAX_ATTENDEES,
                    String.format("%s attendees are already signed up, limit cannot be lowered to %s", attendeesCount, maxAttendees)
            ));
        }
    }

    private static void assertEvent(PostType postType) {
        if (postType != PostType.EVENT) {
            throw new BadRequestException(AppError.of(Code.VALIDATION_ERROR, String.format("%s is not an %s", postType, PostType.EVENT)));
        }
    }

}
