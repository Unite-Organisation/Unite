package com.app.prod.announcements.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AnnouncementDto(
        UUID id,
        String name,
        UUID areaId,
        UUID buildingId,
        UUID createdBy,
        String content,
        LocalDateTime relatedDate,
        LocalDateTime createdAt,
        String photoPath
) {
}
