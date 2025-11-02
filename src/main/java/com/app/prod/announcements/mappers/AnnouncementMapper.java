package com.app.prod.announcements.mappers;

import com.app.prod.announcements.dto.AnnouncementRequest;
import org.jooq.sources.tables.records.AnnouncementsRecord;

import java.time.LocalDateTime;
import java.util.UUID;

public class AnnouncementMapper {

    public static AnnouncementsRecord fromRequestToRecord(AnnouncementRequest request, UUID userId, LocalDateTime now, UUID id){
        return new AnnouncementsRecord(
                id,
                request.name(),
                request.areaId(),
                request.buildingId(),
                userId,
                now,
                request.content(),
                null,
                request.relatedDate()
        );
    }

}
