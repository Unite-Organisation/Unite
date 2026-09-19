package com.app.prod.event.service;

import com.app.prod.event.repository.EventMemberSessionRepository;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.IllegalApplicationStateException;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.EventMemberSessionRecord;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventSessionService {

    public static final String COOKIE_NAME = "eventSession";
    public static final Duration SESSION_TTL = Duration.ofDays(30);
    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EventMemberSessionRepository sessionRepository;

    public String open(UUID memberId, LocalDateTime now) {
        String rawToken = randomToken();
        sessionRepository.insertOne(new EventMemberSessionRecord(
                UUID.randomUUID(),
                memberId,
                hash(rawToken),
                now.plus(SESSION_TTL),
                now
        ));
        return rawToken;
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
}
