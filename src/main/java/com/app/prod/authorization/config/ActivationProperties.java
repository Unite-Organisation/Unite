package com.app.prod.authorization.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "activation")
@Getter
@Setter
public class ActivationProperties {

    private String linkBaseUrl = "http://localhost:3000/activate";

    private Duration tokenTtl = Duration.ofDays(7);
}
