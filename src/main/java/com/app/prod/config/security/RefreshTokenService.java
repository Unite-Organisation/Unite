package com.app.prod.config.security;

import com.app.prod.authorization.repository.RefreshTokenRepository;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.exceptions.exceptions.IllegalApplicationStateException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.RefreshTokenRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    @Getter
    @Value("${jwt.refresh-expiration-ms}")
    private long refreshTokenExpirationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    public RefreshTokenRecord createRefreshToken(UUID userId, String ipAddress, String userAgent) {
        RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setUserId(userId);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(LocalDateTime.ofInstant(Instant.now().plusMillis(refreshTokenExpirationMs), ZoneOffset.UTC));
        refreshToken.setIpAddress(ipAddress);
        refreshToken.setUserAgent(userAgent);
        refreshToken.setIsUsed(false);

        refreshTokenRepository.insertOne(refreshToken);
        return refreshToken;
    }

    @Transactional
    public RefreshTokenRecord rotateRefreshToken(String requestToken, String ipAddress, String userAgent) {
        RefreshTokenRecord oldToken = fetchRefreshToken(requestToken);

        if (Boolean.TRUE.equals(oldToken.getIsUsed()) || oldToken.getRevokedAt() != null) {
            log.warn("Token reuse detected for user {}! Revoking all sessions.", oldToken.getUserId());
            LocalDateTime now = LocalDateTime.now(clock);
            refreshTokenRepository.revokeAllUserTokens(oldToken.getUserId(), now);
            throw new RuntimeException("Security violation. Token reuse detected. Please log in again.");
        }

        if (oldToken.getExpiryDate().isBefore(LocalDateTime.now(clock))) {
            throw new RuntimeException("Refresh token was expired. Please make a new login request");
        }

        if (compareIPs(oldToken.getIpAddress(), ipAddress)) {
            handleDifferentIpCall(oldToken.getIpAddress(), ipAddress);
        }

        if (oldToken.getUserAgent().equals(userAgent)) {
            handleDifferentUserAgentCall(oldToken.getUserAgent(), userAgent);
        }

        oldToken.setIsUsed(true);
        oldToken.setRevokedAt(LocalDateTime.now(ZoneOffset.UTC));
        refreshTokenRepository.update(oldToken);

        return createRefreshToken(oldToken.getUserId(), ipAddress, userAgent);
    }

    private void handleDifferentIpCall(String previousIp, String actualIp) {
        //TODO: Do nothing right now, accept different ip calls
    }

    private void handleDifferentUserAgentCall(String previousUserAgent, String actualUserAgent) {
        //TODO: Do nothing right now, accept different user agent calls
    }

    public void deactivateRefreshToken (String refreshToken) {
        RefreshTokenRecord oldToken = fetchRefreshToken(refreshToken);

        if (Boolean.TRUE.equals(oldToken.getIsUsed()) || oldToken.getRevokedAt() != null) {
            log.warn("Logging out with used token, user {}", oldToken.getUserId());
            //TODO: this should not happen, potential attack (not dangerous)
            return;
        }

        if (oldToken.getExpiryDate().isBefore(LocalDateTime.now(clock))) {
            log.warn("Logging out with expired token, user {}", oldToken.getUserId());
            return;
        }

        oldToken.setIsUsed(true);
        oldToken.setRevokedAt(LocalDateTime.now(ZoneOffset.UTC));
        refreshTokenRepository.update(oldToken);
    }

    private RefreshTokenRecord fetchRefreshToken(String refreshToken) {
        List<RefreshTokenRecord> refreshTokenRecordList = refreshTokenRepository.findByToken(refreshToken);

        if (refreshTokenRecordList.isEmpty()) {
            throw new BadRequestException(AppError.of(Code.REFRESH_TOKEN_ERROR,"Refresh token not found"));
        }
        if (refreshTokenRecordList.size() != 1) {
            throw new IllegalApplicationStateException(AppError.of(Code.REFRESH_TOKEN_ERROR,"More than one token with same string content"));
        }

        return refreshTokenRecordList.getFirst();
    }

    public boolean compareIPs(String ip1, String ip2) {

        InetAddress addr1;
        InetAddress addr2;

        try {
            addr1 = InetAddress.getByName(ip1);
            addr2 = InetAddress.getByName(ip2);

            if (addr1 == null || addr2 == null) {
                throw new UnknownHostException("Unknown host");
            }

        } catch (UnknownHostException e) {
            log.error("Host unknown for either {} or {}", ip1, ip2);
            return false;
        }

        return addr1.equals(addr2);
    }
}
