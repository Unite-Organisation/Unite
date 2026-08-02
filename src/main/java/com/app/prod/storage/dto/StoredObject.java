package com.app.prod.storage.dto;

public record StoredObject(
        String key,
        String contentType,
        long size
) {
}
