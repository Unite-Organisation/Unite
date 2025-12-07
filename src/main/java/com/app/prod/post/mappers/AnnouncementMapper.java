package com.app.prod.post.mappers;

import com.app.prod.post.dto.AnnouncementRequest;
import com.app.prod.post.dto.EventRequest;
import org.jooq.sources.tables.records.PostRecord;

import java.time.LocalDateTime;
import java.util.UUID;

public class AnnouncementMapper {

    public static PostRecord fromRequestToRecordAnn(AnnouncementRequest request, UUID userId, LocalDateTime now, UUID id){
        return new PostRecord(
                id,
                request.name(),
                request.areaId(),
                request.buildingId(),
                userId,
                now,
                request.content(),
                null,
                request.relatedDate(),
                request.postType().name(),
                null,
                null,
                null,
                null,
                null
        );
    }

    public static PostRecord fromRequestToRecordEvent(EventRequest request, UUID userId, LocalDateTime now, UUID id, UUID areaId){
        return new PostRecord(
                id,
                request.name(),
                areaId,
                request.buildingId(),
                userId,
                now,
                request.content(),
                null,
                request.relatedDate(),
                request.postType().name(),
                request.startDate(),
                request.endDate(),
                request.location(),
                request.onlineUrl(),
                request.maxAtendees()
        );
    }

}
