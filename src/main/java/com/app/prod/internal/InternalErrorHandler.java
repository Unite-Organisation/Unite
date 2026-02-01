package com.app.prod.internal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Slf4j
public class InternalErrorHandler implements RestClient.ResponseSpec.ErrorHandler {

    @Override
    public void handle(HttpRequest request, ClientHttpResponse response) throws IOException {
        HttpStatusCode status = response.getStatusCode();

        String responseBody = new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));

        log.error("--- Internal communication error ---");
        log.error("Method: {}", request.getMethod());
        log.error("URI: {}", request.getURI());
        log.error("Status: {}", status.value());
        log.error("Response body: {}", responseBody);
        log.error("------------------------------------");

        // TODO: maybe create some logic for retrying or circuit breaker
        if (status.is4xxClientError()) {}
        else if (status.is5xxServerError()) {}
    }
}
