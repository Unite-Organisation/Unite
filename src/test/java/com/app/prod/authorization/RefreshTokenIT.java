package com.app.prod.authorization;

import com.app.prod.authorization.repository.RefreshTokenRepository;
import com.app.prod.builders.UserPersistanceFactory;
import com.app.prod.config.IntegrationTest;
import com.app.prod.config.security.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import org.jooq.sources.tables.records.RefreshTokenRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A refresh the server turns down is an authentication failure (401), never a server fault - the frontend
 * drops its session hint on 4xx only, so a 500 here would make it retry the dead cookie on every startup.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RefreshTokenIT extends IntegrationTest {

    private static final String USER_AGENT = "refresh-token-it";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UserPersistanceFactory userPersistanceFactory;
    @Autowired
    private RefreshTokenService refreshTokenService;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private Clock clock;

    @Test
    void shouldAnswerARefreshWithoutCookieWithUnauthorized() throws Exception {
        mvc.perform(post("/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors[0].code").value("REFRESH_TOKEN_MISSING"));
    }

    @Test
    void shouldAnswerAReusedTokenWithUnauthorized() throws Exception {
        RefreshTokenRecord token = newToken();
        token.setIsUsed(true);
        token.setRevokedAt(LocalDateTime.now(clock));
        refreshTokenRepository.update(token);

        mvc.perform(post("/auth/refresh").cookie(refreshCookie(token)).header("User-Agent", USER_AGENT))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors[0].code").value("REFRESH_TOKEN_ERROR"));
    }

    @Test
    void shouldAnswerAnExpiredTokenWithUnauthorized() throws Exception {
        RefreshTokenRecord token = newToken();
        token.setExpiryDate(LocalDateTime.now(clock).minusMinutes(1));
        refreshTokenRepository.update(token);

        mvc.perform(post("/auth/refresh").cookie(refreshCookie(token)).header("User-Agent", USER_AGENT))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors[0].code").value("REFRESH_TOKEN_ERROR"));
    }

    @Test
    void shouldLetALogoutWithoutCookieSucceed() throws Exception {
        mvc.perform(post("/auth/logout"))
                .andExpect(status().isOk());
    }

    private RefreshTokenRecord newToken() {
        var user = userPersistanceFactory.getNewUser().withRandomValues().buildAndSave();
        var created = refreshTokenService.createRefreshToken(user.getId(), "127.0.0.1", USER_AGENT);
        // The created record is detached - fetch it back so it can be updated
        return refreshTokenRepository.findByToken(created.getToken()).getFirst();
    }

    private static Cookie refreshCookie(RefreshTokenRecord token) {
        return new Cookie("refreshToken", token.getToken());
    }
}
