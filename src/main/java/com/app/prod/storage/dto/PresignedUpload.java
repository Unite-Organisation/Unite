package com.app.prod.storage.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Everything the client needs to push a single file straight to the storage provider.
 * The method and headers are part of the response so the frontend does not have to
 * assume anything about which provider is behind the url.
 */
public record PresignedUpload(
        String key,
        String uploadUrl,
        String method,
        Map<String, String> requiredHeaders,
        LocalDateTime expiresAt
) {
}
