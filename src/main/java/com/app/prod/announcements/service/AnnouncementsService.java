package com.app.prod.announcements.service;

import com.app.prod.announcements.dto.AnnouncementRequest;
import com.app.prod.announcements.dto.AnnouncementResponse;
import com.app.prod.announcements.mappers.AnnouncementMapper;
import com.app.prod.announcements.repository.AnnouncementsRepository;
import com.app.prod.utils.Pagination;
import com.app.prod.utils.uploads.PhotosUploadService;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.app.prod.utils.uploads.UploadsManager.ANNOUNCEMENTS_PATH;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnnouncementsService {

    private final AnnouncementsRepository announcementsRepository;
    private final PhotosUploadService photosUploadService;
    private final Clock clock;
    private final Validate validate;

    public UUID createAnnouncement(AnnouncementRequest request, UUID userId) {
        //TODO: check if manager can post announcements for building or area

        var now = LocalDateTime.now(clock);
        var id = UUID.randomUUID();
        announcementsRepository.insertOne(AnnouncementMapper.fromRequestToRecord(request, userId, now, id));

        log.info("Created announcement with name: {}", request.name());
        return id;
    }

    public void addImageForAnnouncement(MultipartFile photo, UUID announcementId){
        validate.announcement(announcementId);

        log.info("Uploading photo");
        String photoPath = photosUploadService.uploadFile(ANNOUNCEMENTS_PATH, photo);
        announcementsRepository.updatePhotoPath(announcementId, photoPath);
    }

    public List<AnnouncementResponse> getAnnouncements(Pagination pagination, UUID userId) {
        var announcements = announcementsRepository.findForUser(userId, pagination);
        List<AnnouncementResponse> announcementResponses = new ArrayList<>();

        for(var announcement : announcements){
            announcementResponses.add(new AnnouncementResponse(
                    announcement.id(),
                    announcement.name(),
                    announcement.areaId(),
                    announcement.buildingId(),
                    announcement.createdBy(),
                    announcement.content(),
                    announcement.relatedDate(),
                    announcement.createdAt()
            ));
        }

        return announcementResponses;
    }

    public Pair<byte[], MediaType> getAnnouncementPhoto(UUID id){
        validate.announcement(id);
        var announcement = announcementsRepository.findById(id).get();

        log.info("Reading photo from disk for {}", announcement.getName());
        return readAndFetchPhoto(announcement.getImageReference());
    }

    private Pair<byte[], MediaType> readAndFetchPhoto(String photoPath){
        var photoData = photosUploadService.readFile(photoPath);
        byte[] file = photoData.getLeft();
        MediaType mediaType = photoData.getRight();

        return Pair.of(file, mediaType);
    }
}
