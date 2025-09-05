package com.app.prod.announcements.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AnnouncementRequest(
        String name,
        UUID areaId,
        UUID buildingId,
        String content,
        String url, //TO BE CHANGED
        LocalDateTime relatedDate
) {
}
