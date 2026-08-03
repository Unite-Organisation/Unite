package com.app.prod.utils.filters;

import lombok.Builder;
import org.jooq.Condition;
import org.jooq.Field;

import java.util.Optional;

@Builder
public class ComparisonFilter<T> {
    public Optional<T> value;
    public Modifier modifier;

    public enum Modifier {
        LOWER,
        HIGHER,
        EQUAL
    }

    public boolean isEmpty() {
        return value.isEmpty();
    }

    public static <T> ComparisonFilter<T> empty() {
        return ComparisonFilter.of(null, null);
    }

    public static <T> ComparisonFilter<T> of(T value, Modifier modifier) {
        return ComparisonFilter.<T>builder()
                .value(Optional.ofNullable(value))
                .modifier(modifier == null ? Modifier.EQUAL : modifier)
                .build();
    }

    public Optional<Condition> toCondition(Field<T> field) {
        if (isEmpty()) {
            return Optional.empty();
        }

        var effective = modifier == null ? Modifier.EQUAL : modifier;
        return value.map(r -> switch (effective) {
            case LOWER -> field.le(r);
            case HIGHER -> field.ge(r);
            case EQUAL -> field.eq(r);
        });
    }
}
