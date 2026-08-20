package com.app.prod.authorization.service;

import com.app.prod.authorization.config.ActivationProperties;
import com.app.prod.authorization.dto.IssuedActivationToken;
import com.app.prod.authorization.repository.ActivationTokenRepository;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.exceptions.exceptions.IllegalApplicationStateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.ActivationTokenRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class ActivationTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ActivationTokenRepository activationTokenRepository;
    private final ActivationProperties activationProperties;
    private final Clock clock;

    public List<IssuedActivationToken> issueFor(List<UUID> userIds) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime expiresAt = now.plus(activationProperties.getTokenTtl());

        List<RawToken> rawTokens = userIds.stream()
                .map(userId -> new RawToken(UUID.randomUUID(), userId, randomToken()))
                .toList();

        activationTokenRepository.insertMany(rawTokens.stream()
                .map(token -> new ActivationTokenRecord(
                        token.tokenId(),
                        token.userId(),
                        hash(token.value()),
                        expiresAt,
                        now,
                        null
                ))
                .toList());

        return rawTokens.stream()
                .map(token -> new IssuedActivationToken(token.tokenId(), token.userId(), activationLink(token.value())))
                .toList();
    }

    public UUID consume(String rawToken) {
        ActivationTokenRecord token = activationTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> {
                    log.info("Activation attempted with an unknown token");
                    return new BadRequestException(AppError.of(Code.ACTIVATION_TOKEN_INVALID));
                });

        LocalDateTime now = LocalDateTime.now(clock);
        if (token.getExpiresAt().isBefore(now)) {
            log.info("Activation token {} expired at {}", token.getId(), token.getExpiresAt());
            throw new BadRequestException(AppError.of(Code.ACTIVATION_TOKEN_EXPIRED));
        }

        int burned = activationTokenRepository.markUsed(token.getId(), now);
        if (burned == 0) {
            log.info("Activation token {} was already used", token.getId());
            throw new BadRequestException(AppError.of(Code.ACTIVATION_TOKEN_USED));
        }

        return token.getUserId();
    }

    private String activationLink(String rawToken) {
        return UriComponentsBuilder.fromUriString(activationProperties.getLinkBaseUrl())
                .queryParam("token", rawToken)
                .toUriString();
    }

    private static String randomToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalApplicationStateException(AppError.of(Code.UNKNOWN_ERROR, "SHA-256 is not available"));
        }
    }

    private record RawToken(UUID tokenId, UUID userId, String value) {
    }
}
