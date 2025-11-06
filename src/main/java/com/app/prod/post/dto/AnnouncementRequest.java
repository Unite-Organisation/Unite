package com.app.prod.post.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AnnouncementRequest(
        String name,
        UUID areaId,
        UUID buildingId,
        String content,
        LocalDateTime relatedDate
) {
}
