package com.app.prod.event.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;

/**
 * What a shared event link turns into when a chat app unfurls it. See {@code EventShareRewrite} for
 * how {@code /e/{slug}} gets from the frontend's domain to this backend.
 */
@Component
@ConfigurationProperties(prefix = "event.share")
@Getter
@Setter
public class EventShareProperties {
    /** Where an event lives in the frontend; the slug is appended. Same address the mails link to. */
    private String linkBaseUrl = "http://localhost:4204/e";
    /** The frontend's index.html, at an address the frontend hosting does not rewrite to this backend. */
    private String frontendIndexUrl = "http://localhost:4204/index.html";
    /** How long a fetched index.html is trusted before it is revalidated against the frontend. */
    private Duration indexRefreshInterval = Duration.ofSeconds(30);
    private String siteName = "Unitly";
    private Locale locale = Locale.of("pl", "PL");
}
