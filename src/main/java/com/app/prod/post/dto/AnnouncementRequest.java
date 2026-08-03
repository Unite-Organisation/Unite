package com.app.prod.post.dto;

import com.app.prod.post.enums.PostType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AnnouncementRequest(
        @NotBlank String name,
        UUID areaId,
        UUID buildingId,
        @NotBlank String content,
        LocalDateTime relatedDate,
        @NotNull PostType postType,
        @NotNull LocalDateTime visibleFrom,
        @NotNull LocalDateTime visibleTo,
        @Size(max = 5)
        List<String> fileKeys
) {
}
