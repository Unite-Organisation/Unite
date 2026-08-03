package com.app.prod.storage.file;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

@Getter
public enum FileType {
    PHOTO(Set.of("image/jpeg", "image/png", "image/gif", "image/webp")),
    VIDEO(Set.of("video/mp4", "video/webm")),
    SOUND(Set.of("audio/mpeg", "audio/wav", "audio/ogg", "audio/aac"));

    private final Set<String> mimeTypes;

    FileType(Set<String> mimeTypes) {
        this.mimeTypes = mimeTypes;
    }

    public static Optional<FileType> fromMimeType(String mimeType) {
        if (mimeType == null) {
            return Optional.empty();
        }

        String normalized = mimeType.toLowerCase().split(";")[0].trim();
        return Arrays.stream(values())
                .filter(type -> type.mimeTypes.contains(normalized))
                .findFirst();
    }
}
