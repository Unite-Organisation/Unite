package com.app.prod.authorization.dto;

public record AuthTokensDto(
        String accessToken,
        String refreshToken
) {
}
