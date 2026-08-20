package com.app.prod.requests.service;

import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.requests.repository.RequestDonorRepository;
import com.app.prod.requests.repository.RequestRepository;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.AppUserRecord;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.app.prod.requests.enums.RequestStatus.CANCELLED_BY_AUTHOR;
import static com.app.prod.requests.enums.RequestStatus.CLOSED;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestOperationsService {

    private final Validate validate;
    private final RequestRepository requestRepository;
    private final RequestDonorRepository requestDonorRepository;

    public void cancelRequest(UUID requestId, AppUserRecord user) {
        validate.thatThisRequestBelongsToUser(user, requestId);
        requestRepository.updateStatus(CANCELLED_BY_AUTHOR, requestId);
        log.info("Updated request {} status to {}", requestId, CANCELLED_BY_AUTHOR);
    }

    public void itemReturned(UUID requestId, AppUserRecord user) {
        checkIfUserIsDonorForRequest(user.getId(), requestId);
        requestRepository.updateStatus(CLOSED, requestId);
        log.info("Closed request {} by {}", requestId, user.getId());
    }

    private void checkIfUserIsDonorForRequest(UUID userId, UUID requestId){
        if(!requestDonorRepository.userIsDonorForRequest(userId, requestId)){
            throw new BadRequestException(AppError.of(Code.REQUEST_DONOR));
        }
    }
}
