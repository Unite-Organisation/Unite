package com.app.prod.exceptions.exceptions;

import com.app.prod.exceptions.AppError;

public class FileNotFoundException  extends AppException {
    public FileNotFoundException(AppError... errors) {
        super(errors);
    }
}
