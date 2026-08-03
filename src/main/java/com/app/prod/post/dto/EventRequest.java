package com.app.prod.post.dto;

import com.app.prod.post.enums.PostType;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
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
        Integer maxAtendees,
        @Size(max = 5)
        List<String> fileKeys
) {
}
