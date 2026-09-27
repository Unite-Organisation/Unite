package com.app.prod.utils.filters;

import com.app.prod.utils.Pagination;
import com.app.prod.utils.Search;
import lombok.experimental.SuperBuilder;
import org.jooq.Condition;
import org.jooq.impl.DSL;

import java.util.List;

@SuperBuilder
public abstract class PredicateFilter {
    private final Search search;
    private final Pagination pagination;

    public abstract List<Condition> combineConditions();

    public Condition parseFilter() {
        return combineConditions().stream().reduce(DSL.trueCondition(), Condition::and);
    }

    public Pagination pagination() {
        return pagination == null ? Pagination.defaults() : pagination;
    }

    public Search search() {
        return search == null ? Search.empty() : search;
    }
}
