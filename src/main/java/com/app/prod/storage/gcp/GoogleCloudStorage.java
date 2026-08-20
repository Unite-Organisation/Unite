package com.app.prod.storage.gcp;

import com.app.prod.config.StorageProperties;
import com.app.prod.storage.AbstractStorage;
import com.google.cloud.storage.Storage;

import java.time.Clock;

public abstract class GoogleCloudStorage implements AbstractStorage {

    protected final Storage storage;
    protected final StorageProperties properties;
    protected final Clock clock;

    protected GoogleCloudStorage(Storage storage, StorageProperties properties, Clock clock) {
        this.storage = storage;
        this.properties = properties;
        this.clock = clock;
    }

    protected String bucketName() {
        return properties.getBucket();
    }
}
