package com.app.prod.exceptions.exceptions;

import com.app.prod.exceptions.AppError;

public class DataAlreadyExistsException extends AppException {
    public DataAlreadyExistsException(AppError... errors) {
        super(errors);
    }
}
