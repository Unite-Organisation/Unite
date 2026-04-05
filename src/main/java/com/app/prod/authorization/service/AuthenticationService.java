package com.app.prod.authorization.service;

import com.app.prod.authorization.dto.AuthTokensDto;
import com.app.prod.config.security.RefreshTokenService;
import com.app.prod.config.security.jwt.JwtService;
import com.app.prod.user.dto.UserLoginRequest;
import com.app.prod.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.AppUserRecord;
import org.jooq.sources.tables.records.RefreshTokenRecord;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;

    public AuthTokensDto getTokensRefreshRotate(String oldToken, String ipAddress, String userAgent) {
        RefreshTokenRecord refreshToken = getRefreshTokenAndRotate(oldToken, ipAddress, userAgent);
        AppUserRecord user = userService.findById(refreshToken.getUserId());
        String accessToken = getAccessToken(user.getUsername());
        return new AuthTokensDto(accessToken, refreshToken.getToken());
    }

    public AuthTokensDto getTokensWithoutRefreshingAndRotating(String username, String password, String ipAddress, String userAgent) {
        AppUserRecord user = userService.findByUsername(username);
        String accessToken = getAccessToken(new UserLoginRequest(username, password));
        RefreshTokenRecord refreshToken = getRefreshToken(user.getId(), ipAddress, userAgent);
        return new AuthTokensDto(accessToken, refreshToken.getToken());
    }

    public void deactivateRefreshToken(String refreshToken) {
        refreshTokenService.deactivateRefreshToken(refreshToken);
    }

    public RefreshTokenRecord getRefreshToken(UUID userId, String ipAddress, String userAgent) {
        return refreshTokenService.createRefreshToken(userId, ipAddress, userAgent);
    }

    public RefreshTokenRecord getRefreshTokenAndRotate(String refreshToken, String ipAddress, String userAgent) {
        return refreshTokenService.rotateRefreshToken(refreshToken, ipAddress, userAgent);
    }

    public String getAccessToken(UserLoginRequest request){
        return jwtService.generateJwtAccessToken(request.username(), request.password());
    }

    public String getAccessToken(String username) {
        return jwtService.generateTokenForExistingUser(username);
    }

    public Duration getRefreshTokenStandardDuration() {
        return Duration.ofMillis(refreshTokenService.getRefreshTokenExpirationMs());
    }

    public Duration getAccessTokenStandardDuration() {
        return Duration.ofMillis(jwtService.getAccessTokenExpiration());
    }

}
