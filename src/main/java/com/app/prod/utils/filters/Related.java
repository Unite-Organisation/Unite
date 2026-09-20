package com.app.prod.utils.filters;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;

/**
 * Conditions reaching a table the query does not select from.
 * <p>
 * A filter never adds a join: a join onto a 1:n relation multiplies rows and breaks the
 * {@code offset/limit} paging every filtered endpoint relies on, and it would make the repository
 * query depend on which filters happen to be active. Correlated subqueries keep the single
 * {@code .where(filter.parseFilter())} intact - Postgres plans an {@code EXISTS} as a semi-join,
 * so nothing is lost by expressing it this way.
 */
public final class Related {

    private Related() {
    }

    /** 1:n or n:m - the join that cannot duplicate rows. */
    public static Condition existsIn(Table<?> table, Condition... on) {
        return DSL.exists(DSL.selectOne().from(table).where(on));
    }

    public static Condition notExistsIn(Table<?> table, Condition... on) {
        return DSL.notExists(DSL.selectOne().from(table).where(on));
    }

    /** n:1 - a value from the table next door, usable like any other field. */
    public static <T> Field<T> lookup(Field<T> value, Table<?> from, Condition... on) {
        return DSL.field(DSL.select(value).from(from).where(on));
    }
}
