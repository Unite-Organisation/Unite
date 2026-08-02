package com.app.prod.storage.gcp;

import com.app.prod.config.StorageProperties;
import com.app.prod.storage.dto.PresignedUpload;
import com.app.prod.storage.dto.StoredObject;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@Profile("!test")
public class GoogleCloudStorageService extends GoogleCloudStorage {

    private static final String CONTENT_TYPE_HEADER = "Content-Type";

    public GoogleCloudStorageService(Storage storage, StorageProperties properties, Clock clock) {
        super(storage, properties, clock);
    }

    @Override
    public PresignedUpload createUploadUrl(String key, String contentType, Duration ttl) {
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName(), key))
                .setContentType(contentType)
                .build();

        URL signedUrl = storage.signUrl(
                blobInfo,
                ttl.toSeconds(),
                TimeUnit.SECONDS,
                Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                Storage.SignUrlOption.withContentType(),
                Storage.SignUrlOption.withV4Signature()
        );

        log.info("Created upload url for {}", key);
        return new PresignedUpload(
                key,
                signedUrl.toString(),
                HttpMethod.PUT.name(),
                Map.of(CONTENT_TYPE_HEADER, contentType),
                LocalDateTime.now(clock).plus(ttl)
        );
    }

    @Override
    public Optional<StoredObject> find(String key) {
        Blob blob = storage.get(BlobId.of(bucketName(), key));

        if (blob == null || !blob.exists()) {
            return Optional.empty();
        }

        long size = blob.getSize() == null ? 0 : blob.getSize();
        return Optional.of(new StoredObject(key, blob.getContentType(), size));
    }

    @Override
    public String getPrivateFileUrl(String key) {
        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName(), key).build();

        URL signedUrl = storage.signUrl(
                blobInfo,
                properties.getDownloadUrlTtl().toSeconds(),
                TimeUnit.SECONDS,
                Storage.SignUrlOption.withV4Signature()
        );

        return signedUrl.toString();
    }

    @Override
    public void delete(String key) {
        boolean deleted = storage.delete(BlobId.of(bucketName(), key));
        log.info("Deletion of {} from Google Cloud Storage returned {}", key, deleted);
    }
}
