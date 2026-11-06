package com.app.prod.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * One problem with a request.
 *
 * @param field      the request property the problem is about, as the client sent it - a JSON key, or a path
 *                   such as {@code slots[1].startDate} for nested values. Absent when the error is not about a
 *                   single input, so a client can highlight exactly the inputs that carry one.
 * @param constraint the bean validation constraint that failed ({@code Size}, {@code NotBlank}, ...), only on
 *                   annotation based validation errors
 * @param params     the constraint's attributes worth showing to a user, e.g. {@code {"min": 0, "max": 256}}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AppError(
        @JsonProperty("code") Code code,
        String message,
        String field,
        String constraint,
        Map<String, Object> params
) {
    public AppError(Code code) {
        this(code, code.getDefaultMessage());
    }

    public AppError(Code code, String message) {
        this(code, message, null, null, null);
    }

    public static AppError of(Code code) {
        return new AppError(code);
    }

    public static AppError of(Code code, String message) {
        return new AppError(code, message);
    }

    public static AppError ofField(Code code, String field, String message) {
        return new AppError(code, message, field, null, null);
    }

    public static AppError ofConstraint(Code code, String field, String message, String constraint, Map<String, Object> params) {
        return new AppError(code, message, field, constraint, params == null || params.isEmpty() ? null : params);
    }
}
