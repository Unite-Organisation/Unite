package com.app.prod.utils;

import java.util.Locale;

/**
 * The one form an address is compared in. Providers treat addresses case-insensitively, so
 * {@code Anna@Example.com} and {@code anna@example.com} are the same person and must never become
 * two accounts or slip past a ban. Queries compare {@code lower(column)} against this.
 */
public final class Emails {

    private Emails() {
    }

    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
