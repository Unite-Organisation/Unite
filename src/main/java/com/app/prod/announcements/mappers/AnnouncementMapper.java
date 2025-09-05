package com.app.prod.announcements.mappers;

import com.app.prod.announcements.dto.AnnouncementRequest;
import org.jooq.sources.tables.records.AnnouncementsRecord;

import java.time.LocalDateTime;
import java.util.UUID;

public class AnnouncementMapper {

    public static AnnouncementsRecord fromRequestToRecord(AnnouncementRequest request, UUID userId, LocalDateTime now){
        return new AnnouncementsRecord(
                UUID.randomUUID(),
                request.name(),
                request.areaId(),
                request.buildingId(),
                userId,
                now,
                request.content(),
                request.url(),
                request.relatedDate()
        );
    }

}
