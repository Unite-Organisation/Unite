package com.app.prod.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

/**
 * Storage settings shared by every {@link com.app.prod.storage.AbstractStorage} implementation,
 * so that switching the cloud provider only means swapping the implementation bean.
 */
@Component
@ConfigurationProperties(prefix = "storage")
@Getter
@Setter
public class StorageProperties {

    private String bucket;

    private Duration uploadUrlTtl = Duration.ofMinutes(15);

    private Duration downloadUrlTtl = Duration.ofMinutes(15);

    private DataSize maxFileSize = DataSize.ofMegabytes(50);
}
