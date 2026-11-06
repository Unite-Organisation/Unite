package com.app.prod.exceptions;

import com.app.prod.config.IntegrationTest;
import com.app.prod.event.dto.CreateEventRequest;
import com.app.prod.event.dto.SlotRequest;
import com.app.prod.event.enums.SchedulingMode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every validation error names the input it is about, so the frontend can highlight exactly that input.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ValidationErrorResponseIT extends IntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldNameTheFieldAndTheRuleOfAnAnnotationConstraint() throws Exception {
        CreateEventRequest request = new CreateEventRequest("Barbecue", null, LocalDateTime.now().plusDays(7), null,
                "x".repeat(257), null, null, false, "Kuba", "kuba@test.local", null, null, null, null, null, false);

        createEvent(objectMapper.writeValueAsString(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("location"))
                .andExpect(jsonPath("$.errors[0].constraint").value("Size"))
                .andExpect(jsonPath("$.errors[0].params.max").value(256))
                .andExpect(jsonPath("$.errors[0].message").value(not("location")));
    }

    @Test
    void shouldNameEveryFieldABusinessRuleIsAbout() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(7);
        CreateEventRequest request = new CreateEventRequest("Volleyball", null, null, null, null, null,
                null, false, "Kuba", "kuba@test.local", null, SchedulingMode.POLL, null, null,
                List.of(new SlotRequest(start, null), new SlotRequest(start.plusDays(1), null)), false);

        createEvent(objectMapper.writeValueAsString(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("minAttendees", "votingDeadline")));
    }

    @Test
    void shouldPointAtTheProposedDateThatBreaksARule() throws Exception {
        LocalDateTime deadline = LocalDateTime.now().plusDays(2);
        CreateEventRequest request = new CreateEventRequest("Volleyball", null, null, null, null, null,
                null, false, "Kuba", "kuba@test.local", null, SchedulingMode.POLL, 3, deadline,
                List.of(new SlotRequest(deadline.plusDays(5), null), new SlotRequest(deadline.minusDays(1), null)), false);

        createEvent(objectMapper.writeValueAsString(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_TIME_PERIOD"))
                .andExpect(jsonPath("$.errors[0].field").value("slots[1].startDate"));
    }

    @Test
    void shouldAnswerAValueOfTheWrongTypeWithABadRequestNamingTheField() throws Exception {
        createEvent("""
                {"name": "Barbecue", "maxAttendees": "plenty", "displayName": "Kuba", "email": "kuba@test.local"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("maxAttendees"))
                .andExpect(jsonPath("$.errors[0].message").value(not(containsString("java."))));
    }

    @Test
    void shouldAnswerMalformedJsonWithABadRequest() throws Exception {
        createEvent("{\"name\": ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").doesNotExist());
    }

    private ResultActions createEvent(String body) throws Exception {
        return mvc.perform(post("/public/event").contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
