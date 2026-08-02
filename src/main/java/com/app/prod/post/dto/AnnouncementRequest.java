package com.app.prod.post.dto;

import com.app.prod.post.enums.PostType;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AnnouncementRequest(
        String name,
        UUID areaId,
        UUID buildingId,
        String content,
        LocalDateTime relatedDate,
        PostType postType,
        @Size(max = 5)
        List<String> fileKeys
) {
}
