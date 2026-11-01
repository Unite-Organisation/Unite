package com.app.prod.exceptions.exceptions;

import com.app.prod.exceptions.AppError;
import lombok.Getter;


@Getter
public class TooManyRequestsException extends AppException {

    private final long retryAfterSeconds;

    public TooManyRequestsException(long retryAfterSeconds, AppError... errors) {
        super(errors);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
