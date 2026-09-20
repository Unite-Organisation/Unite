package com.app.prod.exceptions.exceptions;

import com.app.prod.exceptions.AppError;

public class InvalidFileExtensionException  extends AppException {
    public InvalidFileExtensionException(AppError... errors) {
        super(errors);
    }
}
