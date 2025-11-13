package com.app.prod.requests.service;

import com.app.prod.conversation.service.ConversationService;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.requests.dto.RequestHelpRequest;
import com.app.prod.requests.dto.RequestHelpResponse;
import com.app.prod.requests.enums.RequestStatus;
import com.app.prod.requests.repository.RequestDonorRepository;
import com.app.prod.requests.repository.RequestRepository;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.AppUserRecord;
import org.jooq.sources.tables.records.RequestDonorRecord;
import org.jooq.sources.tables.records.RequestRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestDonorService {

    private final Clock clock;
    private final Validate validate;
    private final ConversationService conversationService;
    private final RequestDonorRepository requestDonorRepository;
    private final RequestService requestService;
    private final RequestRepository requestRepository;

    @Transactional
    public RequestHelpResponse helpWithRequest(AppUserRecord user, RequestHelpRequest helpRequest) {
        var request = requestService.getRequest(helpRequest.requestId());
        validate.thatUserBelongsToArea(request.getAreaId(), user);
        checkIfRequestIsActive(request);

        //TODO: notify user by notifications logic (after implementing notifications for residents)

        requestRepository.updateStatus(RequestStatus.ITEM_PROVIDED, helpRequest.requestId());
        requestDonorRepository.insertOne(createRequestDonorRecord(request, user, helpRequest.deadline()));
        log.info("User {} offered help with request {}", user.getId(), request.getId());

        //TODO: create conversation for those users if not exist
        var conversationId = conversationService.getConversationId(request.getUserInNeed(), user.getId());
        return new RequestHelpResponse(conversationId);
    }

    private void checkIfRequestIsActive(RequestRecord request){
        boolean active = request.getIsActive();
        boolean statusCondition = request.getStatus().equals(RequestStatus.CREATED);

        if(!(active && statusCondition)){
            throw new BadRequestException(String.format("Request %s is already handled", request.getId()));
        }
    }

    private RequestDonorRecord createRequestDonorRecord(RequestRecord requestRecord, AppUserRecord donor, LocalDateTime deadline){
        LocalDateTime now = LocalDateTime.now(clock);
        return new RequestDonorRecord(
                UUID.randomUUID(),
                requestRecord.getId(),
                donor.getId(),
                now,
                deadline
        );
    }
}
