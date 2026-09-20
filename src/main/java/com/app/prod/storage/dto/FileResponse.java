package com.app.prod.storage.dto;

public record FileResponse(
        String key,
        String url,
        String contentType
) {
}
