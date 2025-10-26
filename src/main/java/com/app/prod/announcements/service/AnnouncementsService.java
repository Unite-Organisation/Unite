package com.app.prod.announcements.service;

import com.app.prod.announcements.dto.AnnouncementDto;
import com.app.prod.announcements.dto.AnnouncementRequest;
import com.app.prod.announcements.dto.AnnouncementResponse;
import com.app.prod.announcements.mappers.AnnouncementMapper;
import com.app.prod.announcements.repository.AnnouncementsRepository;
import com.app.prod.utils.Pagination;
import com.app.prod.utils.uploads.PhotosUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public void createAnnouncement(AnnouncementRequest request, MultipartFile photo, UUID userId) {
        //TODO: check if manager can post announcements for building or area

        log.info("Uploading photo");
        String photoPath = photosUploadService.uploadFile(ANNOUNCEMENTS_PATH, photo);

        var now = LocalDateTime.now(clock);
        announcementsRepository.insertOne(AnnouncementMapper.fromRequestToRecord(request, userId, now, photoPath));

        log.info("Created announcement with name: {}", request.name());
    }

    public List<AnnouncementResponse> getAnnouncements(Pagination pagination, UUID userId) {
        var announcements = announcementsRepository.findForUser(userId, pagination);
        List<AnnouncementResponse> announcementResponses = new ArrayList<>();

        for(var announcement : announcements){
            log.info("Reading photo from disk for {}", announcement.name());

            var photoData = photosUploadService.readFile(announcement.photoPath());
            byte[] file = photoData.getLeft();
            MediaType mediaType = photoData.getRight();

            announcementResponses.add(new AnnouncementResponse(
                    announcement.id(),
                    announcement.name(),
                    announcement.areaId(),
                    announcement.buildingId(),
                    announcement.createdBy(),
                    announcement.content(),
                    announcement.relatedDate(),
                    announcement.createdAt(),
                    file,
                    mediaType
            ));
        }

        return announcementResponses;
    }
}
