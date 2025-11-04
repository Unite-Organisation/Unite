package com.app.prod.issues;

import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.issues.service.IssueStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.app.prod.issues.enums.IssueProcessingStatus.*;
import static com.app.prod.user.enums.UserRole.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class IssueStatusServiceTest {

    private IssueStatusService issueStatusService;

    @BeforeEach
    void setUp() {
        issueStatusService = new IssueStatusService();
    }

    // -----------------------
    // COMMON VALIDATION
    // -----------------------

    @Test
    void shouldThrowException_WhenProvidedStatusIsSubmitted() {
        assertThatThrownBy(() ->
                issueStatusService.checkIfStatusUpdateIsPossible(SUBMITTED, SUBMITTED, RESIDENT)
        ).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("It is not possible to change status to SUBMITTED");
    }

    @Test
    void shouldThrowException_WhenProvidedStatusIsSeenByRecipient() {
        assertThatThrownBy(() ->
                issueStatusService.checkIfStatusUpdateIsPossible(SUBMITTED, SEEN_BY_RECIPIENT, MANAGER)
        ).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("It is not possible to change status to SUBMITTED or SEEN_BY_RECIPIENT");
    }

    // -----------------------
    // RESIDENT
    // -----------------------

    @Test
    void shouldThrowException_WhenResidentTriesToChangeStatusToOtherThanClosed() {
        assertThatThrownBy(() ->
                issueStatusService.checkIfStatusUpdateIsPossible(TAKEN_ACTION, RESOLVED, RESIDENT)
        ).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Resident can change status only to CLOSED");
    }

    @Test
    void shouldNotThrow_WhenResidentChangesStatusToClosed() {
        assertThatCode(() ->
                issueStatusService.checkIfStatusUpdateIsPossible(RESOLVED, CLOSED, RESIDENT)
        ).doesNotThrowAnyException();
    }

    // -----------------------
    // MANAGER
    // -----------------------

    @Test
    void shouldThrowException_WhenManagerTriesToChangeFromSubmittedToResolved() {
        assertThatThrownBy(() ->
                issueStatusService.checkIfStatusUpdateIsPossible(SUBMITTED, RESOLVED, MANAGER)
        ).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("It is not possible to change status to RESOLVED yet");
    }

    @Test
    void shouldNotThrow_WhenManagerChangesFromSubmittedToTakenAction() {
        assertThatCode(() ->
                issueStatusService.checkIfStatusUpdateIsPossible(SUBMITTED, TAKEN_ACTION, MANAGER)
        ).doesNotThrowAnyException();
    }

    @Test
    void shouldThrowException_WhenManagerTriesToChangeFromTakenActionToClosed() {
        assertThatThrownBy(() ->
                issueStatusService.checkIfStatusUpdateIsPossible(TAKEN_ACTION, CLOSED, MANAGER)
        ).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("After issue is marked as TAKEN_ACTION, only state it can change to is RESOLVED");
    }

    @Test
    void shouldNotThrow_WhenManagerChangesFromTakenActionToResolved() {
        assertThatCode(() ->
                issueStatusService.checkIfStatusUpdateIsPossible(TAKEN_ACTION, RESOLVED, MANAGER)
        ).doesNotThrowAnyException();
    }

    // -----------------------
    // ADMIN
    // -----------------------

    @Test
    void shouldNotThrow_WhenAdminChangesAnyStatus() {
        assertThatCode(() ->
                issueStatusService.checkIfStatusUpdateIsPossible(RESOLVED, CLOSED, ADMIN)
        ).doesNotThrowAnyException();

        assertThatCode(() ->
                issueStatusService.checkIfStatusUpdateIsPossible(SUBMITTED, RESOLVED, ADMIN)
        ).doesNotThrowAnyException();
    }

}
