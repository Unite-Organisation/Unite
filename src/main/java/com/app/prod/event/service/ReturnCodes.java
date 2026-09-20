package com.app.prod.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class ReturnCodes {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final BCryptPasswordEncoder encoder;

    public IssuedCode issue() {
        String code = String.format("%04d", RANDOM.nextInt(10_000));
        return new IssuedCode(code, encoder.encode(code));
    }

    public boolean matches(String code, String hash) {
        return encoder.matches(code, hash);
    }

    public record IssuedCode(String code, String hash) {
    }
}
