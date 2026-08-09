package com.app.prod.post.dto;

import com.app.prod.post.enums.PostType;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record EventRequest(
        String name,
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
