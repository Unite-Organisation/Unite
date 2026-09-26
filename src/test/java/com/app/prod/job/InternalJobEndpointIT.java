package com.app.prod.job;

import com.app.prod.config.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InternalJobEndpointIT extends IntegrationTest {

    private static final String TOKEN = "test-drain-token";
    private static final String HEADER = "X-Job-Token";

    @Autowired
    private MockMvc mvc;

    @Test
    void shouldRejectDrainWithoutAToken() throws Exception {
        mvc.perform(post("/internal/jobs/drain"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectDrainWithTheWrongToken() throws Exception {
        mvc.perform(post("/internal/jobs/drain").header(HEADER, TOKEN + "-nope"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectDrainWithAnEmptyToken() throws Exception {
        mvc.perform(post("/internal/jobs/drain").header(HEADER, ""))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAcceptDrainWithTheConfiguredToken() throws Exception {
        mvc.perform(post("/internal/jobs/drain").header(HEADER, TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimed").exists())
                .andExpect(jsonPath("$.succeeded").exists())
                .andExpect(jsonPath("$.failed").exists());
    }
}
