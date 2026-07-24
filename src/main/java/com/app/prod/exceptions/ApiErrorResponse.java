package com.app.prod.exceptions;

import lombok.Builder;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

@Builder
public record ApiErrorResponse(
        int status,
        List<AppError> errors,
        String path,
        Instant timestamp
) {
    public static ApiErrorResponse of(HttpStatus status, List<AppError> errors, String path) {
        return ApiErrorResponse.builder()
                .status(status.value())
                .errors(errors)
                .path(path)
                .timestamp(Instant.now())
                .build();
    }

    public static ApiErrorResponse of(HttpStatus status, AppError error, String path) {
        return ApiErrorResponse.builder()
                .status(status.value())
                .errors(List.of(error))
                .path(path)
                .timestamp(Instant.now())
                .build();
    }
}
