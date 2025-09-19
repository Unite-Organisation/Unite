package com.app.prod.announcements.service;

import com.app.prod.announcements.dto.AnnouncementRequest;
import com.app.prod.announcements.dto.AnnouncementResponse;
import com.app.prod.announcements.mappers.AnnouncementMapper;
import com.app.prod.announcements.repository.AnnouncementsRepository;
import com.app.prod.utils.Pagination;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnnouncementsService {

    private final AnnouncementsRepository announcementsRepository;
    private final Clock clock;;

    public void createAnnouncement(AnnouncementRequest request, UUID userId) {
        //TODO: check if manager can post announcements for builidng or area
        var now = LocalDateTime.now(clock);
        announcementsRepository.insertOne(AnnouncementMapper.fromRequestToRecord(request, userId, now));

        log.info("Created announcement with name: {}", request.name());
    }

    public List<AnnouncementResponse> getAnnouncements(Pagination pagination, UUID userId) {
        return announcementsRepository.findForUser(userId, pagination);
    }
}
