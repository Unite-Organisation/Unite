package com.app.prod.utils.filters;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.Optional;

@Builder
public class PriceFilter {
    public Optional<BigDecimal> price;
    public PriceModifier priceModifier;

    public enum PriceModifier {
        LOWER,
        HIGHER,
        EQUAL
    }
}