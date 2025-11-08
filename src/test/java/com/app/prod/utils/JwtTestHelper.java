package com.app.prod.utils;

import com.app.prod.user.enums.UserRole;
import com.app.prod.user.repository.UserRepository;
import com.app.prod.user.repository.UserRoleRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.UsersRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Helper class for generating JWT tokens in integration tests.
 * This allows tests to authenticate as specific users without going through the full login flow.
 */
@Component
@RequiredArgsConstructor
public class JwtTestHelper {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expiration;

    private Key key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate a JWT token for a user by their UUID.
     * The user must exist in the database.
     *
     * @param userId The UUID of the user
     * @return JWT token string
     * @throws RuntimeException if user is not found
     */
    public String generateTokenForUser(UUID userId) {
        UsersRecord user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        return generateToken(user.getUsername(), getUserRole(user));
    }

    /**
     * Generate a JWT token for a user record.
     *
     * @param user The user record
     * @param role The user's role
     * @return JWT token string
     */
    public String generateTokenForUser(UsersRecord user, UserRole role) {
        return generateToken(user.getUsername(), role);
    }

    /**
     * Generate a JWT token with a specific username and role.
     * Useful for testing without needing an actual user in the database.
     *
     * @param username The username
     * @param role The user role
     * @return JWT token string
     */
    public String generateToken(String username, UserRole role) {
        Instant now = Instant.now();
        String roleWithPrefix = "ROLE_" + role.name();

        return Jwts.builder()
                .setSubject(username)
                .claim("role", roleWithPrefix)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(expiration)))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Get the role of a user from the database.
     *
     * @param user The user record
     * @return The user's role
     */
    private UserRole getUserRole(UsersRecord user) {
        return userRoleRepository.findById(user.getUserRole())
                .map(record -> UserRole.valueOf(record.getUserRole()))
                .orElseThrow(() -> new RuntimeException("User role not found for user: " + user.getUsername()));
    }
}

