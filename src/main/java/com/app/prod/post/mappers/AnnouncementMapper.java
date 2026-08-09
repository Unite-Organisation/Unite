package com.app.prod.post.mappers;

import com.app.prod.access.BuildingScope;
import com.app.prod.post.dto.AnnouncementRequest;
import com.app.prod.post.dto.EventRequest;
import org.jooq.JSONB;
import org.jooq.sources.tables.records.PostRecord;

import java.time.LocalDateTime;
import java.util.UUID;

public class AnnouncementMapper {

    public static PostRecord fromRequestToRecordAnn(AnnouncementRequest request, BuildingScope scope, LocalDateTime now, UUID id, JSONB attachments){
        PostRecord record = basePost(scope, now, id, attachments);

        record.setName(request.name());
        record.setContent(request.content());
        record.setRelatedDate(request.relatedDate());
        record.setPostType(request.postType().name());
        record.setVisibleFrom(request.visibleFrom());
        record.setVisibleTo(request.visibleTo());

        return record;
    }

    public static PostRecord fromRequestToRecordEvent(EventRequest request, BuildingScope scope, LocalDateTime now, UUID id, JSONB attachments){
        PostRecord record = basePost(scope, now, id, attachments);

        record.setName(request.name());
        record.setContent(request.content());
        record.setRelatedDate(request.relatedDate());
        record.setPostType(request.postType().name());
        record.setStartDateTime(request.startDate());
        record.setEndDateTime(request.endDate());
        record.setLocationName(request.location());
        record.setOnlineUrl(request.onlineUrl());
        record.setMaxAttendees(request.maxAtendees());

        return record;
    }

    private static PostRecord basePost(BuildingScope scope, LocalDateTime now, UUID id, JSONB attachments) {
        PostRecord record = new PostRecord();

        record.setId(id);
        record.setBuildingId(scope.buildingId());
        record.setCreatedBy(scope.userId());
        record.setCreatedAt(now);
        record.setAttachments(attachments);

        return record;
    }

}
