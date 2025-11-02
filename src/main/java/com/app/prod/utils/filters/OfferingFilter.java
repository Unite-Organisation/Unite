package com.app.prod.utils.filters;

import com.app.prod.offering.enums.OfferingCategory;
import lombok.Builder;

import java.util.Optional;

@Builder
public class OfferingFilter {
    public Optional<OfferingCategory> category;
    public PriceFilter price;
}
