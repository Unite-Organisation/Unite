package com.app.prod.user.dto;

public record UserLoginRequest(
        String username,
        String password
) {
}
