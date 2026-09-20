package com.app.prod.exceptions;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AppError(
        @JsonProperty("code") Code code,
        String message
) {
    public AppError(Code code) {
        this(code, code.getDefaultMessage());
    }

    public static AppError of(Code code) {
        return new AppError(code);
    }

    public static AppError of(Code code, String message) {
        return new AppError(code, message);
    }
}
