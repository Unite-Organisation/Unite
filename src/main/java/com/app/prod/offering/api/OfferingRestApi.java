package com.app.prod.offering.api;

import com.app.prod.config.security.GlobalSecurityManager;
import com.app.prod.offering.dto.OfferingRequest;
import com.app.prod.offering.service.OfferingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("offering")
@RequiredArgsConstructor
@Tag(name = "User offerings")
public class OfferingRestApi {

    private final OfferingService offeringService;
    private final GlobalSecurityManager globalSecurityManager;

    @PostMapping()
    @PreAuthorize("hasRole('RESIDENT')")
    public void createOffering(@RequestBody @Valid OfferingRequest offeringRequest){
        var user = globalSecurityManager.getCurrentUser();
        offeringService.createOffering(offeringRequest, user);
    }

    @GetMapping()
    @PreAuthorize("hasRole('RESIDENT')")
    public void getOfferings(@RequestBody @Valid OfferingRequest offeringRequest){
        var user = globalSecurityManager.getCurrentUser();
        offeringService.createOffering(offeringRequest, user);
    }


}
