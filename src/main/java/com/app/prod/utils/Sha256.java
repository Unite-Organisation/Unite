package com.app.prod.utils;

import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.IllegalApplicationStateException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class Sha256 {

    private Sha256() {
    }

    public static String hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalApplicationStateException(AppError.of(Code.UNKNOWN_ERROR, "SHA-256 is not available"));
        }
    }

    public static String hexOrNull(String value) {
        return value == null || value.isBlank() ? null : hex(value);
    }
}
