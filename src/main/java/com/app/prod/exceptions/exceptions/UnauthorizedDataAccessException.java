package com.app.prod.exceptions.exceptions;

import com.app.prod.exceptions.AppError;

public class UnauthorizedDataAccessException extends AppException {
    public UnauthorizedDataAccessException(AppError... errors) {
        super(errors);
    }
}
