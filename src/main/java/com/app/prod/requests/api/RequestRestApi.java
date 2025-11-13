package com.app.prod.requests.api;

import com.app.prod.config.security.GlobalSecurityManager;
import com.app.prod.requests.dto.RequestRequest;
import com.app.prod.requests.service.RequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("request")
public class RequestRestApi {

    private final GlobalSecurityManager globalSecurityManager;
    private final RequestService requestService;

    @PostMapping()
    public void createRequest(@RequestBody RequestRequest request){
        var user = globalSecurityManager.getCurrentUser();
        requestService.createRequest(request, user);
    }

    @PatchMapping("/cancel")
    public void cancelRequest(@RequestParam UUID requestId){
        var user = globalSecurityManager.getCurrentUser();
        requestService.cancelRequest(requestId, user);
    }

    @PostMapping("/{requestId}/help")
    public void helpWithRequest(){
        var user = globalSecurityManager.getCurrentUser();
        requestService.helpWithRequest(requestId, user);
    }

}
