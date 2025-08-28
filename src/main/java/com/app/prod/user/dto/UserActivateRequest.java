package com.app.prod.user.dto;

public record UserActivateRequest(
        String temporaryLogin,
        String temporaryPassword,
        String username,
        String email,
        String password
) {
}
