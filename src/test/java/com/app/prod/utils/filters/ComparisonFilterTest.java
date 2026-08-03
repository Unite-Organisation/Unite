package com.app.prod.utils.filters;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.sources.Tables.OFFERING;

class ComparisonFilterTest {

    private static final BigDecimal PRICE = new BigDecimal("100.00");

    @Test
    void shouldProduceNoConditionWithoutValue() {
        assertThat(ComparisonFilter.<BigDecimal>empty().toCondition(OFFERING.PRICE)).isEmpty();
        assertThat(ComparisonFilter.of((BigDecimal) null, ComparisonFilter.Modifier.LOWER).toCondition(OFFERING.PRICE)).isEmpty();
    }

    @Test
    void shouldMapLowerToLessOrEqual() {
        var condition = ComparisonFilter.of(PRICE, ComparisonFilter.Modifier.LOWER).toCondition(OFFERING.PRICE);

        assertThat(condition).hasValueSatisfying(c -> assertThat(c).hasToString(OFFERING.PRICE.le(PRICE).toString()));
    }

    @Test
    void shouldMapHigherToGreaterOrEqual() {
        var condition = ComparisonFilter.of(PRICE, ComparisonFilter.Modifier.HIGHER).toCondition(OFFERING.PRICE);

        assertThat(condition).hasValueSatisfying(c -> assertThat(c).hasToString(OFFERING.PRICE.ge(PRICE).toString()));
    }

    @Test
    void shouldMapEqualToEquality() {
        var condition = ComparisonFilter.of(PRICE, ComparisonFilter.Modifier.EQUAL).toCondition(OFFERING.PRICE);

        assertThat(condition).hasValueSatisfying(c -> assertThat(c).hasToString(OFFERING.PRICE.eq(PRICE).toString()));
    }

    @Test
    void shouldFallBackToEqualWhenModifierIsMissing() {
        var condition = ComparisonFilter.of(PRICE, null).toCondition(OFFERING.PRICE);

        assertThat(condition).hasValueSatisfying(c -> assertThat(c).hasToString(OFFERING.PRICE.eq(PRICE).toString()));
    }
}
