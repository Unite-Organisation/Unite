package com.app.prod.utils.filters;

import org.jooq.Condition;
import org.jooq.Field;

import java.util.Optional;

/**
 * A {@link Filter} comparing with an operator instead of equality - the value plus the
 * {@code <field>Modifier} request param that says which way to compare it.
 */
public class ComparisonFilter<T> extends Filter<T> {

    private final Modifier modifier;

    public enum Modifier {
        LESS_OR_EQUAL_THAN,
        GREATER_OR_EQUAL_THAN,
        EQUAL
    }

    private ComparisonFilter(T value, Modifier modifier) {
        super(value);
        this.modifier = modifier == null ? Modifier.EQUAL : modifier;
    }

    public static <T> ComparisonFilter<T> of(T value, Modifier modifier) {
        return new ComparisonFilter<>(value, modifier);
    }

    public static <T> ComparisonFilter<T> empty() {
        return new ComparisonFilter<>(null, null);
    }

    @Override
    public Optional<Condition> toCondition(Field<T> field) {
        return value().map(v -> switch (modifier) {
            case LESS_OR_EQUAL_THAN -> field.le(v);
            case GREATER_OR_EQUAL_THAN -> field.ge(v);
            case EQUAL -> field.eq(v);
        });
    }
}
