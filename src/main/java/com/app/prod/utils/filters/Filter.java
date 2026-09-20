package com.app.prod.utils.filters;

import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.IllegalApplicationStateException;
import org.jooq.Condition;
import org.jooq.Field;

import java.util.Optional;

/**
 * One field of a filter: a value that may be absent, plus the way it turns into a jOOQ condition.
 * <p>
 * Absent - a null field or a null value - means "do not narrow the result", so a filter can gain a
 * field without touching the places that build it. Subclasses change how the value is compared:
 * {@link ComparisonFilter} swaps equality for a range, and the filter declaring the field does not
 * need to know which of the two it got.
 */
public class Filter<T> {

    protected final T value;

    protected Filter(T value) {
        this.value = value;
    }

    public static <T> Filter<T> of(T value) {
        return new Filter<>(value);
    }

    public static <T> Filter<T> empty() {
        return new Filter<>(null);
    }

    public boolean isEmpty() {
        return value == null;
    }

    public Optional<T> value() {
        return Optional.ofNullable(value);
    }

    public T require(String name) {
        if (isEmpty()) {
            throw new IllegalApplicationStateException(AppError.of(
                    Code.MANDATORY_FILTER_MISSING,
                    String.format("%s has no value, the query would not be narrowed by it at all", name)
            ));
        }
        return value;
    }
    
    public Optional<Condition> toCondition(Field<T> field) {
        return value().map(field::eq);
    }
}
