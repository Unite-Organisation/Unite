package com.app.prod.utils.filters;

import org.jooq.Condition;
import org.jooq.Field;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Factories for the criteria a {@link PredicateFilter} is built from, so a filter field costs
 * exactly one line and no filter hand-rolls a condition list.
 * <p>
 * How a value is compared is the {@link Filter}'s business, not this class's - which is why one
 * {@code match} covers a plain field and a {@link ComparisonFilter} range alike.
 */
public final class Criteria {

    private Criteria() {
    }

    public static List<Condition> of(Criterion... criteria) {
        return Arrays.stream(criteria)
                .map(Criterion::toCondition)
                .flatMap(Optional::stream)
                .toList();
    }

    /** Any filter field - equality, a range, whatever the Filter subtype does. */
    public static <T> Criterion match(Field<T> field, Filter<T> filter) {
        return () -> empty(filter) ? Optional.empty() : filter.toCondition(field);
    }

    /**
     * Enums are stored as their name, so they need their own factory - a {@code match} overload
     * taking {@code Filter<E>} would clash with {@link #match(Field, Filter)} after erasure.
     */
    public static <E extends Enum<E>> Criterion matchEnum(Field<String> field, Filter<E> filter) {
        return () -> empty(filter) ? Optional.empty() : filter.value().map(e -> field.eq(e.name()));
    }

    /**
     * A field the query must not run without - the buildingId settled by an authorized BuildingScope
     * above all. Missing, it fails the request instead of silently widening the result to every row.
     */
    public static <T> Criterion required(Field<T> field, Filter<T> filter) {
        return () -> {
            Filter<T> present = filter == null ? Filter.empty() : filter;
            present.require(field.getName());
            return present.toCondition(field);
        };
    }

    /** Always applied, regardless of any request param - e.g. a fixed time window. */
    public static Criterion always(Condition condition) {
        return () -> Optional.of(condition);
    }

    /**
     * Escape hatch for anything that is not a comparison of this field - a mapped enum, or a
     * condition reaching another table through {@link Related}.
     */
    public static <T> Criterion when(Filter<T> filter, Function<T, Condition> mapper) {
        return () -> empty(filter) ? Optional.empty() : filter.value().map(mapper);
    }

    /**
     * A field left unset on the @Builder is null, not an empty Filter. Treating that as "do not
     * narrow the result" is what lets a filter gain a field without touching every place it is built.
     */
    private static boolean empty(Filter<?> filter) {
        return filter == null || filter.isEmpty();
    }
}
