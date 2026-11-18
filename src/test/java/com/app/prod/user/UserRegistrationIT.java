package com.app.prod.user;

import com.app.prod.config.IntegrationTest;
import com.app.prod.user.enums.UserRole;
import com.app.prod.user.repository.UserRepository;
import com.app.prod.user.service.UserRoleService;
import org.jooq.sources.tables.records.AppUserRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Registering is public, so it can neither hand out roles nor let one person hold two accounts
 * under one address written in different case.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserRegistrationIT extends IntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserRoleService userRoleService;

    @Test
    void whoeverRegistersIsAResidentWhateverRoleTheyAskFor() throws Exception {
        String suffix = suffix();

        register("admin" + suffix, "admin" + suffix + "@test.local", "ADMIN").andExpect(status().isOk());

        AppUserRecord user = userRepository.findByUsername("admin" + suffix).orElseThrow();
        assertThat(user.getUserRole()).isEqualTo(userRoleService.getUserRoleId(UserRole.RESIDENT));
    }

    @Test
    void theAddressIsStoredLowerCasedAndTrimmed() throws Exception {
        String suffix = suffix();

        register("anna" + suffix, "  Anna" + suffix + "@Test.Local ", null).andExpect(status().isOk());

        assertThat(userRepository.findByUsername("anna" + suffix).orElseThrow().getEmail())
                .isEqualTo("anna" + suffix + "@test.local");
    }

    @Test
    void anAddressDifferingOnlyInCaseIsAlreadyTaken() throws Exception {
        String suffix = suffix();
        register("anna" + suffix, "anna" + suffix + "@test.local", null).andExpect(status().isOk());

        register("other" + suffix, "ANNA" + suffix + "@Test.Local", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0].code").value("EMAIL_TAKEN"));
    }

    private ResultActions register(String username, String email, String role) throws Exception {
        String roleField = role == null ? "" : String.format(", \"role\": \"%s\"", role);
        String body = String.format("""
                {"firstName": "Anna", "lastName": "Nowak", "username": "%s", "email": "%s", "password": "Secret123!"%s}
                """, username, email, roleField);
        return mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
