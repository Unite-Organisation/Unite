package com.app.prod.post.service;

import com.app.prod.post.dto.AnnouncementRequest;
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

@RequiredArgsConstructor
@Slf4j
@Service
public class AnnouncementService {

    private final PostRepository postRepository;
    private final FileService fileService;
    private final Clock clock;

    public EntityCreatedResponse createAnnouncement(AnnouncementRequest request, UUID userId) {
        //TODO: check if manager can post announcements for building or area

        var attachments = fileService.confirmUploaded(ContextStoragePrefix.ANNOUNCEMENT, userId, request.fileKeys());
        var now = LocalDateTime.now(clock);
        var id = UUID.randomUUID();
        postRepository.insertOne(AnnouncementMapper.fromRequestToRecordAnn(request, userId, now, id, attachments));

        log.info("Created announcement with name: {}", request.name());
        return new EntityCreatedResponse(id);
    }

}
