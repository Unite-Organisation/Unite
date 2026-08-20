package com.app.prod.exceptions.exceptions;

import com.app.prod.exceptions.AppError;

public class IllegalApplicationStateException  extends AppException {
    public IllegalApplicationStateException(AppError... errors) {
        super(errors);
    }
}
