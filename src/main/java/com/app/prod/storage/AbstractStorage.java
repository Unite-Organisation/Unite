package com.app.prod.storage;

import com.app.prod.storage.dto.PresignedUpload;
import com.app.prod.storage.dto.StoredObject;

import java.time.Duration;
import java.util.Optional;

public interface AbstractStorage {

    PresignedUpload createUploadUrl(String key, String contentType, Duration ttl);

    Optional<StoredObject> find(String key);

    void move(String sourceKey, String targetKey);

    String getPrivateFileUrl(String key);

    void delete(String key);
}
