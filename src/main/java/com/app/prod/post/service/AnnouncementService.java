package com.app.prod.post.service;

import com.app.prod.access.BuildingScope;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.post.dto.AnnouncementRequest;
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

@RequiredArgsConstructor
@Slf4j
@Service
public class AnnouncementService {

    private final PostRepository postRepository;
    private final FileService fileService;
    private final Clock clock;

    public EntityCreatedResponse createAnnouncement(AnnouncementRequest request, BuildingScope scope) {
        validateVisibilityWindow(request.visibleFrom(), request.visibleTo());

        var attachments = fileService.confirmUploaded(ContextStoragePrefix.ANNOUNCEMENT, scope.userId(), request.fileKeys());
        var now = LocalDateTime.now(clock);
        var id = UUID.randomUUID();

        try {
            postRepository.insertOne(AnnouncementMapper.fromRequestToRecordAnn(request, scope, now, id, attachments));
        } catch (RuntimeException exception) {
            fileService.discard(attachments);
            throw exception;
        }

        log.info("Created announcement with name: {} in building: {}", request.name(), scope.buildingId());
        return new EntityCreatedResponse(id);
    }

    private static void validateVisibilityWindow(LocalDateTime visibleFrom, LocalDateTime visibleTo) {
        if (visibleFrom == null || visibleTo == null) {
            return;
        }

        if (visibleFrom.isAfter(visibleTo)) {
            log.warn("Visible from: {} is after visible to: {}", visibleFrom, visibleTo);
            throw new BadRequestException(AppError.of(Code.INVALID_TIME_PERIOD, String.format("%s is after %s", visibleFrom, visibleTo)));
        }
    }

    @Transactional
    public void updateAnnouncement(UUID id, AnnouncementRequest request, BuildingScope scope) {
        assertAnnouncement(request.postType());
        validateVisibilityWindow(request.visibleFrom(), request.visibleTo());

        PostRecord post = postRepository.findInBuildingForUpdate(scope.buildingId(), id)
                .orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.POST_NOT_FOUND)));

        post.setName(request.name());
        post.setContent(request.content());
        post.setRelatedDate(request.relatedDate());
        post.setVisibleFrom(request.visibleFrom());
        post.setVisibleTo(request.visibleTo());

        //TODO: files update not supported yet
        postRepository.update(post);
        log.info("Updated announcement: {} in building: {}", id, scope.buildingId());
    }

    private static void assertAnnouncement(PostType postType) {
        if (postType != PostType.ANNOUNCEMENT) {
            throw new BadRequestException(AppError.of(Code.VALIDATION_ERROR, String.format("%s is not an %s", postType, PostType.ANNOUNCEMENT)));
        }
    }

}
