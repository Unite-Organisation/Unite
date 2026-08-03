package com.app.prod.storage;

import com.app.prod.storage.file.FileType;
import lombok.Getter;

import java.util.Set;

/**
 * Describes what a given part of the application is allowed to upload.
 * The prefix is also the first segment of every object key produced for that context.
 */
@Getter
public enum ContextStoragePrefix {
    ANNOUNCEMENT("announcement", 5, Set.of(FileType.PHOTO)),
    EVENT("event", 5, Set.of(FileType.PHOTO));

    private final String prefix;
    private final int maxFiles;
    private final Set<FileType> allowedTypes;

    ContextStoragePrefix(String prefix, int maxFiles, Set<FileType> allowedTypes) {
        this.prefix = prefix;
        this.maxFiles = maxFiles;
        this.allowedTypes = allowedTypes;
    }

    public boolean allows(FileType type) {
        return allowedTypes.contains(type);
    }
}
