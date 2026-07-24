package com.app.prod.exceptions.exceptions;

import com.app.prod.exceptions.AppError;

public class InternalConnectionException extends AppException {
    public InternalConnectionException(AppError... errors) {
        super(errors);
    }
}
