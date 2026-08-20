package com.app.prod.facilities.dto;

import com.app.prod.facilities.enums.FacilityType;
import com.app.prod.utils.filters.ComparisonFilter;
import com.app.prod.utils.filters.FilterRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class FacilityFilterRequest extends FilterRequest {
    private FacilityType facilityType;
    private Boolean requiresApproval;
    private Integer capacity;
    private ComparisonFilter.Modifier capacityModifier;
}
