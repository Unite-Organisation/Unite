package com.app.prod.mail.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Locale;


@Component
@ConfigurationProperties(prefix = "mail")
@Getter
@Setter
public class MailProperties {
    private String from = "no-reply@unite.app";
    private String fromName = "Unite";
    private Locale locale = Locale.of("pl");
}
