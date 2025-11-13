package com.app.prod.requests.api;

import com.app.prod.config.security.GlobalSecurityManager;
import com.app.prod.requests.dto.RequestHelpRequest;
import com.app.prod.requests.dto.RequestHelpResponse;
import com.app.prod.requests.dto.RequestRequest;
import com.app.prod.requests.dto.RequestResponse;
import com.app.prod.requests.enums.RequestStatus;
import com.app.prod.requests.service.RequestDonorService;
import com.app.prod.requests.service.RequestOperationsService;
import com.app.prod.requests.service.RequestService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("request")
@Tag(name = "Item requests")
public class RequestRestApi {

    private final GlobalSecurityManager globalSecurityManager;
    private final RequestService requestService;
    private final RequestDonorService requestDonorService;
    private final RequestOperationsService requestOperationsService;

    @PostMapping()
    @PreAuthorize("hasRole('RESIDENT')")
    public void createRequest(@RequestBody RequestRequest request){
        var user = globalSecurityManager.getCurrentUser();
        requestService.createRequest(request, user);
    }

    @GetMapping()
    public List<RequestResponse> getAllRequests(@RequestParam RequestStatus status){
        var user = globalSecurityManager.getCurrentUser();
        return requestService.getAllRequests(user, status);
    }

    @GetMapping("/mine")
    public List<RequestResponse> getAllRequests(){
        var user = globalSecurityManager.getCurrentUser();
        return requestService.getMyRequests(user);
    }

    @PatchMapping("/cancel")
    public void cancelRequest(@RequestParam UUID requestId){
        var user = globalSecurityManager.getCurrentUser();
        requestOperationsService.cancelRequest(requestId, user);
    }

    @PostMapping("/help")
    @PreAuthorize("hasRole('RESIDENT')")
    public RequestHelpResponse helpWithRequest(@RequestBody RequestHelpRequest request){
        var user = globalSecurityManager.getCurrentUser();
        return requestDonorService.helpWithRequest(user, request);
    }

    @PutMapping("/{requestId}/item-returned")
    @PreAuthorize("hasRole('RESIDENT')")
    public void itemReturned(@PathVariable UUID requestId){
        var user = globalSecurityManager.getCurrentUser();
        requestOperationsService.itemReturned(requestId, user);
    }
}
