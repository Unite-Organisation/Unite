package com.app.prod.utils;

import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.IllegalApplicationStateException;

import java.util.Arrays;
import java.util.Optional;

public enum AppProfile {
    DEV("dev"),
    TEST("test"),
    PROD("prod");

    private final String value;

    AppProfile(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Optional<AppProfile> match(String profileName) {
        return Arrays.stream(values())
                .filter(profile -> profile.value.equalsIgnoreCase(profileName))
                .findFirst();
    }

    public static AppProfile fromString(String profileName) {
        return match(profileName)
                .orElseThrow(() -> new IllegalApplicationStateException(AppError.of(Code.APP_PROFILE_NOT_FOUND)));
    }
}
