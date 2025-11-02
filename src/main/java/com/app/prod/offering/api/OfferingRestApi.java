package com.app.prod.offering.api;

import com.app.prod.building.service.BuildingService;
import com.app.prod.config.security.GlobalSecurityManager;
import com.app.prod.offering.dto.OfferingRequest;
import com.app.prod.offering.dto.OfferingResponse;
import com.app.prod.offering.enums.OfferingCategory;
import com.app.prod.offering.service.OfferingFilteringService;
import com.app.prod.offering.service.OfferingService;
import com.app.prod.utils.filters.OfferingFilter;
import com.app.prod.utils.filters.PriceFilter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jdk.jfr.Frequency;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("offering")
@RequiredArgsConstructor
@Tag(name = "User offerings")
public class OfferingRestApi {

    private final OfferingService offeringService;
    private final GlobalSecurityManager globalSecurityManager;
    private final OfferingFilteringService offeringFilteringService;

    @PostMapping()
    @PreAuthorize("hasRole('RESIDENT')")
    public void createOffering(@RequestBody @Valid OfferingRequest offeringRequest){
        var user = globalSecurityManager.getCurrentUser();
        offeringService.createOffering(offeringRequest, user);
    }

    @GetMapping()
    @PreAuthorize("hasRole('RESIDENT')")
    public List<OfferingResponse> getOfferings(
            @RequestParam(required = false) OfferingCategory category,
            @RequestParam(required = true) BigDecimal price,
            @RequestParam(required = true) PriceFilter.PriceModifier modifier
            ){
        var user = globalSecurityManager.getCurrentUser();
        OfferingFilter filter = offeringFilteringService.prepareFilter(user, category, price, modifier);
        return offeringService.getOfferings(filter);
    }

}
