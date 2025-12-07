package com.app.prod.post.dto;

import com.app.prod.post.enums.PostType;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventRequest(
        String name,
        UUID buildingId,
        String content,
        LocalDateTime relatedDate,
        PostType postType,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String location,
        String onlineUrl,
        Integer maxAtendees
) {
}
