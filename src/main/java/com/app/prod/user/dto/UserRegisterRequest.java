package com.app.prod.user.dto;

public record UserRegisterRequest(
        String firstName,
        String lastName,
        String username,
        String email,
        String password
) {
}
