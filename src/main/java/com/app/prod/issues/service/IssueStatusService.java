package com.app.prod.issues.service;

import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.issues.enums.IssueProcessingStatus;
import com.app.prod.user.enums.UserRole;
import org.springframework.stereotype.Service;

import static com.app.prod.issues.enums.IssueProcessingStatus.SUBMITTED;
import static com.app.prod.issues.enums.IssueProcessingStatus.CLOSED;
import static com.app.prod.issues.enums.IssueProcessingStatus.RESOLVED;
import static com.app.prod.issues.enums.IssueProcessingStatus.TAKEN_ACTION;
import static com.app.prod.issues.enums.IssueProcessingStatus.SEEN_BY_RECIPIENT;

@Service
public class IssueStatusService {

    public void checkIfStatusUpdateIsPossible(
            IssueProcessingStatus issueStatus,
            IssueProcessingStatus providedStatus,
            UserRole userRole
    ){
        if(providedStatus.equals(SUBMITTED) || providedStatus.equals(SEEN_BY_RECIPIENT)){
            throw new BadRequestException(
                    String.format("It is not possible to change status to %s or %s",
                            SUBMITTED.name(),
                            SEEN_BY_RECIPIENT.name()
                    ));
        }

        switch (userRole){
            case RESIDENT -> handleResident(issueStatus, providedStatus);
            case MANAGER -> handleManager(issueStatus, providedStatus);
            case ADMIN -> {}
        }
    }

    private void handleResident(
            IssueProcessingStatus issueStatus,
            IssueProcessingStatus providedStatus
    ){
        if(!providedStatus.equals(CLOSED)){
            throw new BadRequestException(
                    String.format("Resident can change status only to %s", CLOSED.name()));
        }
    }

    private void handleManager(
            IssueProcessingStatus issueStatus,
            IssueProcessingStatus providedStatus
    ){

        if((issueStatus.equals(SUBMITTED) || issueStatus.equals(SEEN_BY_RECIPIENT)) &&
                !providedStatus.equals(TAKEN_ACTION)){
            throw new BadRequestException(
                    String.format("It is not possible to change status to %s yet", providedStatus.name()));
        }

        if(issueStatus.equals(TAKEN_ACTION) && !providedStatus.equals(RESOLVED)){
            throw new BadRequestException(
                    String.format("After issue is marked as %s, only state it can change to is %s",
                            TAKEN_ACTION.name(),
                            RESOLVED.name()
                    ));
        }
    }
}
