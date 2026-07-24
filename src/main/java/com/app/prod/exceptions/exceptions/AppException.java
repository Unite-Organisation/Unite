package com.app.prod.exceptions.exceptions;

import com.app.prod.exceptions.AppError;
import java.util.List;

public class AppException extends RuntimeException {

    List<AppError> appErrors;

    public AppException(String message) {
        super(message);
    }

    public AppException(String message, AppError... appError) {
        super(message);
        appErrors = List.of(appError);
    }

    public AppException(AppError... appError) {
        super("Default app exception");
        appErrors = List.of(appError);
    }

    public AppException(List<AppError> appErrors) {
        super("Default app exception");
        this.appErrors = appErrors;
    }

    public List<AppError> getAppErrors() {
        return appErrors;
    }

}
