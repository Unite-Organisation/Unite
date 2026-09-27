package com.app.prod.utils;

public record Search(String value) {

    public static final char ESCAPE = '!';

    public static Search of(String value) {
        return new Search(value == null || value.isBlank() ? null : value.trim());
    }

    public static Search empty() {
        return new Search(null);
    }

    public boolean isEmpty() {
        return value == null;
    }

    /** The value as a {@code LIKE} pattern - {@code %typed%}, wildcards in the input escaped. */
    public String pattern() {
        String escaped = value
                .replace(String.valueOf(ESCAPE), ESCAPE + String.valueOf(ESCAPE))
                .replace("%", ESCAPE + "%")
                .replace("_", ESCAPE + "_");
        return "%" + escaped + "%";
    }
}
