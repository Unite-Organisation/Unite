package com.app.prod.post.service;

import com.app.prod.access.BuildingScope;
import com.app.prod.eventbus.EventBus;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.exceptions.exceptions.UnauthorizedDataAccessException;
import com.app.prod.post.dto.AnnouncementRequest;
import com.app.prod.post.dto.PostResponse;
import com.app.prod.post.mappers.AnnouncementMapper;
import com.app.prod.post.repository.PostRepository;
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

import static com.app.prod.storage.ContextStoragePrefix.ANNOUNCEMENT;

@RequiredArgsConstructor
@Slf4j
@Service
public class AnnouncementService {

    private final PostRepository postRepository;
    private final FileService fileService;
    private final Clock clock;
    private final EventBus eventBus;
    private final PostService postService;

    public EntityCreatedResponse createAnnouncement(AnnouncementRequest request, BuildingScope scope) {
        validateVisibilityWindow(request.visibleFrom(), request.visibleTo());

        var attachments = fileService.confirmUploaded(ANNOUNCEMENT, scope.userId(), request.fileKeys());
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

    public PostResponse updateAnnouncement(BuildingScope scope, @Valid AnnouncementRequest request) {
        PostRecord post = postRepository.findById(request.id()).orElseThrow(() -> new BadRequestException(new AppError(Code.POST_NOT_FOUND)));

        if (!post.getCreatedBy().equals(scope.userId())) {
            throw new UnauthorizedDataAccessException(AppError.of(Code.NOT_OWN_RESOURCE));
        }

        post.setName(request.name());
        post.setContent(request.content());
        post.setRelatedDate(request.relatedDate());
        post.setVisibleFrom(request.visibleFrom());
        post.setVisibleTo(request.visibleTo());

        List<String> actualKeys = fileService.toStoredFiles(post.getAttachments()).stream().map(StoredFile::key).toList();
        List<String> requestedKeys = request.fileKeys();

        List<String> removedKeys = actualKeys.stream()
                .filter(actualKey -> !requestedKeys.contains(actualKey))
                .toList();

        JSONB attachments = fileService.confirmUploaded(ANNOUNCEMENT, scope.userId(), requestedKeys);
        post.setAttachments(attachments);
        postRepository.update(post);

        eventBus.publish(new RemoveFilesEvent(removedKeys));

        log.info("Updated announcement with id: {} in building: {}", request.id(), scope.buildingId());
        return postService.getPost(scope, request.id());
    }
}
