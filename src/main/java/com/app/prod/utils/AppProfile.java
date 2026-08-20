package com.app.prod.utils;

import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.IllegalApplicationStateException;

public enum AppProfile {
    DEV("dev"),
    PROD("prod");

    private final String value;

    AppProfile(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AppProfile fromString(String profileName) {
        for (AppProfile profile : AppProfile.values()) {
            if (profile.value.equalsIgnoreCase(profileName)) {
                return profile;
            }
        }
        throw new IllegalApplicationStateException(AppError.of(Code.APP_PROFILE_NOT_FOUND));
    }
}
