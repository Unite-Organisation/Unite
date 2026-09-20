package com.app.prod.exceptions.exceptions;

import com.app.prod.exceptions.AppError;

public class AuthenticationFailedException extends AppException {
    public AuthenticationFailedException(AppError... errors) {
        super(errors);
    }
}
