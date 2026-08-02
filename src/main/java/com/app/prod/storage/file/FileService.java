package com.app.prod.storage.file;

import com.app.prod.config.StorageProperties;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.storage.AbstractStorage;
import com.app.prod.storage.ContextStoragePrefix;
import com.app.prod.storage.StorageKeys;
import com.app.prod.storage.dto.FileResponse;
import com.app.prod.storage.dto.FileUploadRequest;
import com.app.prod.storage.dto.PresignedUpload;
import com.app.prod.storage.dto.StoredFile;
import com.app.prod.storage.dto.StoredObject;
import com.app.prod.utils.json.JsonbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.JSONB;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileService {

    private final AbstractStorage storage;
    private final JsonbService jsonbService;
    private final StorageProperties properties;
    private final Clock clock;

    public List<PresignedUpload> createUploadUrls(ContextStoragePrefix context, UUID userId, List<FileUploadRequest> files) {
        if (files == null || files.isEmpty()) {
            throw new BadRequestException(AppError.of(Code.EMPTY_FILE, "No files were requested"));
        }

        validateFileCount(context, files.size());
        files.forEach(file -> validateRequestedFile(context, file));

        return files.stream()
                .map(file -> storage.createUploadUrl(
                        StorageKeys.build(context, userId, file.fileName()),
                        file.contentType(),
                        properties.getUploadUrlTtl()
                ))
                .toList();
    }

    public JSONB confirmUploaded(ContextStoragePrefix context, UUID userId, List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return jsonbService.toJsonb(List.of());
        }

        validateFileCount(context, keys.size());
        validateNoDuplicates(keys);

        var now = LocalDateTime.now(clock);
        List<StoredFile> attachments = keys.stream()
                .map(key -> toStoredFile(context, userId, key, now))
                .toList();

        log.info("Confirmed {} uploaded files for context {}", attachments.size(), context);
        return jsonbService.toJsonb(attachments);
    }

    public List<FileResponse> toResponses(JSONB attachments) {
        return jsonbService.listFromJsonb(attachments, StoredFile.class).stream()
                .map(file -> new FileResponse(file.key(), storage.getPrivateFileUrl(file.key()), file.contentType()))
                .toList();
    }

    private StoredFile toStoredFile(ContextStoragePrefix context, UUID userId, String key, LocalDateTime now) {
        if (!StorageKeys.belongsTo(key, context, userId)) {
            log.warn("User {} sent a file key that does not belong to them: {}", userId, key);
            throw new BadRequestException(AppError.of(Code.INVALID_FILE_KEY));
        }

        StoredObject object = storage.find(key)
                .orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.FILE_NOT_FOUND, "File %s was not uploaded".formatted(key))));

        validateStoredObject(context, object);
        return new StoredFile(object.key(), object.contentType(), object.size(), now);
    }

    private void validateRequestedFile(ContextStoragePrefix context, FileUploadRequest file) {
        if (file.size() == null || file.size() <= 0) {
            throw new BadRequestException(AppError.of(Code.EMPTY_FILE));
        }

        validateSize(file.size());
        validateContentType(context, file.contentType());
    }

    private void validateStoredObject(ContextStoragePrefix context, StoredObject object) {
        if (object.size() <= 0) {
            throw new BadRequestException(AppError.of(Code.EMPTY_FILE));
        }

        validateSize(object.size());
        validateContentType(context, object.contentType());
    }

    private void validateContentType(ContextStoragePrefix context, String contentType) {
        FileType type = FileType.fromMimeType(contentType)
                .orElseThrow(() -> {
                    log.warn("Attempt to use an unsupported file type {}", contentType);
                    return new BadRequestException(AppError.of(Code.UNSUPPORTED_FILE_TYPE, "File type %s is not supported".formatted(contentType)));
                });

        if (!context.allows(type)) {
            throw new BadRequestException(AppError.of(Code.UNSUPPORTED_FILE_TYPE, "%s files are not allowed for %s".formatted(type, context)));
        }
    }

    private void validateSize(long size) {
        if (size > properties.getMaxFileSize().toBytes()) {
            throw new BadRequestException(AppError.of(Code.FILE_TOO_LARGE));
        }
    }

    private void validateFileCount(ContextStoragePrefix context, int count) {
        if (count > context.getMaxFiles()) {
            throw new BadRequestException(AppError.of(Code.TOO_MANY_FILES,
                    "At most %d files are allowed for %s".formatted(context.getMaxFiles(), context)));
        }
    }

    private void validateNoDuplicates(List<String> keys) {
        Set<String> unique = new HashSet<>(keys);

        if (unique.size() != keys.size()) {
            throw new BadRequestException(AppError.of(Code.INVALID_FILE_KEY, "Duplicated file keys"));
        }
    }
}
