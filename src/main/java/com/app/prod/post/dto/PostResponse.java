package com.app.prod.post.dto;

import com.app.prod.post.enums.PostType;
import com.app.prod.storage.dto.FileResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PostResponse(
        UUID id,
        String name,
        UUID areaId,
        UUID buildingId,
        UUID createdBy,
        String content,
        LocalDateTime relatedDate,
        LocalDateTime createdAt,
        PostType postType,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String locationName,
        String onlineUrl,
        Integer maxAttendees,
        List<FileResponse> files
) {
}
