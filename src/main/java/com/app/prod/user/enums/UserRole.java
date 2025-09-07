package com.app.prod.user.enums;

public enum UserRole {
    RESIDENT,
    MANAGER,
    ADMIN;

    public static UserRole fromString(String roleString) {
        if (roleString == null) {
            throw new IllegalArgumentException("Role string cannot be null");
        }
        try {
            return UserRole.valueOf(roleString.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown role: " + roleString);
        }
    }
}
