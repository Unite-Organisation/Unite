package com.app.prod.exceptions.exceptions;

import com.app.prod.exceptions.AppError;

public class EntityNotPresentException  extends AppException {
    public EntityNotPresentException(AppError... errors) {
        super(errors);
    }
}
