package com.app.prod.announcements.dto;

import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.UUID;

public record AnnouncementResponse(
        UUID id,
        String name,
        UUID areaId,
        UUID buildingId,
        UUID createdBy,
        String content,
        LocalDateTime relatedDate,
        LocalDateTime createdAt,
        byte[] photo,
        MediaType photoFileType
) {
}
