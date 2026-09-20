package com.app.prod.issues;

import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.AppException;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.issues.service.IssueStatusService;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.app.prod.issues.enums.IssueProcessingStatus.*;
import static com.app.prod.user.enums.UserRole.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;

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
        assertIssueStatusError(
                () -> issueStatusService.checkIfStatusUpdateIsPossible(SUBMITTED, SUBMITTED, RESIDENT),
                "It is not possible to change status to SUBMITTED"
        );
    }

    @Test
    void shouldThrowException_WhenProvidedStatusIsSeenByRecipient() {
        assertIssueStatusError(
                () -> issueStatusService.checkIfStatusUpdateIsPossible(SUBMITTED, SEEN_BY_RECIPIENT, MANAGER),
                "It is not possible to change status to SUBMITTED or SEEN_BY_RECIPIENT"
        );
    }

    // -----------------------
    // RESIDENT
    // -----------------------

    @Test
    void shouldThrowException_WhenResidentTriesToChangeStatusToOtherThanClosed() {
        assertIssueStatusError(
                () -> issueStatusService.checkIfStatusUpdateIsPossible(TAKEN_ACTION, RESOLVED, RESIDENT),
                "Resident can change status only to CLOSED"
        );
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
        assertIssueStatusError(
                () -> issueStatusService.checkIfStatusUpdateIsPossible(SUBMITTED, RESOLVED, MANAGER),
                "It is not possible to change status to RESOLVED yet"
        );
    }

    @Test
    void shouldNotThrow_WhenManagerChangesFromSubmittedToTakenAction() {
        assertThatCode(() ->
                issueStatusService.checkIfStatusUpdateIsPossible(SUBMITTED, TAKEN_ACTION, MANAGER)
        ).doesNotThrowAnyException();
    }

    @Test
    void shouldThrowException_WhenManagerTriesToChangeFromTakenActionToClosed() {
        assertIssueStatusError(
                () -> issueStatusService.checkIfStatusUpdateIsPossible(TAKEN_ACTION, CLOSED, MANAGER),
                "After issue is marked as TAKEN_ACTION, only state it can change to is RESOLVED"
        );
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

    private static void assertIssueStatusError(ThrowingCallable call, String expectedMessage) {
        Throwable thrown = catchThrowable(call);

        assertThat(thrown).isInstanceOf(BadRequestException.class);
        assertThat(((AppException) thrown).getAppErrors())
                .singleElement()
                .satisfies(error -> {
                    assertThat(error.code()).isEqualTo(Code.ISSUE_STATUS_ERROR);
                    assertThat(error.message()).contains(expectedMessage);
                });
    }

}
