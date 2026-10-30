package com.app.prod.event.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;

/**
 * What a shared event link turns into when a chat app unfurls it. See {@code EventShareRewrite} for
 * how {@code /event/{slug}} gets from the frontend's domain to this backend.
 */
@Component
@ConfigurationProperties(prefix = "event.share")
@Getter
@Setter
public class EventShareProperties {
    private String linkBaseUrl = "http://localhost:4204/event";
    private String frontendIndexUrl = "http://localhost:4204/index.html";
    private Duration indexRefreshInterval = Duration.ofSeconds(60);
    private String siteName = "Unitly";
    private Locale locale = Locale.of("pl", "PL");
}
