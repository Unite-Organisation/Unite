package com.app.prod.exceptions.exceptions;

import com.app.prod.exceptions.AppError;

public class BadRequestException extends AppException {
    public BadRequestException(AppError... errors) {
        super(errors);
    }
}
