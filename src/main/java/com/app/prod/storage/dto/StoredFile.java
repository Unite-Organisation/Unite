package com.app.prod.storage.dto;

import java.time.LocalDateTime;

public record StoredFile(
        String key,
        String contentType,
        long size,
        LocalDateTime uploadedAt
) {
}
