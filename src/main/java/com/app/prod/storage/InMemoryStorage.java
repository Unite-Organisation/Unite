package com.app.prod.storage;

import com.app.prod.storage.dto.PresignedUpload;
import com.app.prod.storage.dto.StoredObject;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


@Service
@Profile("test")
@RequiredArgsConstructor
public class InMemoryStorage implements AbstractStorage {

    private static final String UPLOAD_URL_TEMPLATE = "http://localhost/fake-upload/%s";
    private static final String DOWNLOAD_URL_TEMPLATE = "http://localhost/fake-download/%s";

    private final Map<String, StoredObject> objects = new ConcurrentHashMap<>();
    private final Clock clock;

    @Override
    public PresignedUpload createUploadUrl(String key, String contentType, Duration ttl) {
        return new PresignedUpload(
                key,
                UPLOAD_URL_TEMPLATE.formatted(key),
                "PUT",
                Map.of("Content-Type", contentType),
                LocalDateTime.now(clock).plus(ttl)
        );
    }

    @Override
    public Optional<StoredObject> find(String key) {
        return Optional.ofNullable(objects.get(key));
    }

    @Override
    public void move(String sourceKey, String targetKey) {
        StoredObject object = objects.remove(sourceKey);

        if (object != null) {
            objects.put(targetKey, new StoredObject(targetKey, object.contentType(), object.size()));
        }
    }

    @Override
    public String getPrivateFileUrl(String key) {
        return DOWNLOAD_URL_TEMPLATE.formatted(key);
    }

    @Override
    public void delete(String key) {
        objects.remove(key);
    }

    public void put(String key, String contentType, long size) {
        objects.put(key, new StoredObject(key, contentType, size));
    }

    public void clear() {
        objects.clear();
    }
}
