package com.app.prod.event.service;

import java.security.SecureRandom;
import java.util.Base64;

public final class EventSlug {

    private static final int SLUG_BYTES = 9;
    private static final SecureRandom RANDOM = new SecureRandom();

    private EventSlug() {
    }

    public static String generate() {
        byte[] bytes = new byte[SLUG_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
