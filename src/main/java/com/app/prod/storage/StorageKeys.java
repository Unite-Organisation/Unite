package com.app.prod.storage;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Builds and validates object keys. The owner is part of the key
 * ({@code context/userId/uuid-name}), which is what lets the backend confirm that a key sent back
 * by the client belongs to the caller without keeping any state about issued upload urls.
 */
public final class StorageKeys {

    private static final Pattern UNSAFE_CHARACTERS = Pattern.compile("[^a-zA-Z0-9._-]");
    private static final int MAX_FILE_NAME_LENGTH = 100;
    private static final String FALLBACK_FILE_NAME = "file";

    private StorageKeys() {
    }

    public static String build(ContextStoragePrefix context, UUID userId, String fileName) {
        return "%s/%s/%s-%s".formatted(context.getPrefix(), userId, UUID.randomUUID(), sanitize(fileName));
    }

    public static boolean belongsTo(String key, ContextStoragePrefix context, UUID userId) {
        if (key == null || key.isBlank() || key.contains("..")) {
            return false;
        }

        String expectedPrefix = "%s/%s/".formatted(context.getPrefix(), userId);
        if (!key.startsWith(expectedPrefix)) {
            return false;
        }

        String objectName = key.substring(expectedPrefix.length());
        return !objectName.isBlank() && !objectName.contains("/");
    }

    static String sanitize(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return FALLBACK_FILE_NAME;
        }

        String withoutPath = fileName.substring(Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\')) + 1);
        String sanitized = UNSAFE_CHARACTERS.matcher(withoutPath).replaceAll("_");

        if (sanitized.isBlank() || sanitized.chars().allMatch(character -> character == '.')) {
            return FALLBACK_FILE_NAME;
        }

        return sanitized.length() > MAX_FILE_NAME_LENGTH
                ? sanitized.substring(sanitized.length() - MAX_FILE_NAME_LENGTH)
                : sanitized;
    }
}
