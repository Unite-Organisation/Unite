package com.app.prod.exceptions.exceptions;

import com.app.prod.exceptions.AppError;

public class EmptyFileException  extends AppException {
    public EmptyFileException(AppError... errors) {
        super(errors);
    }
}
