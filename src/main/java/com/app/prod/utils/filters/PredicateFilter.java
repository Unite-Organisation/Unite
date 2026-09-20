package com.app.prod.utils.filters;

import org.jooq.Condition;
import org.jooq.impl.DSL;

import java.util.List;

public interface PredicateFilter {
    List<Condition> combineConditions();

    default Condition parseFilter() {
        return combineConditions().stream().reduce(DSL.trueCondition(), Condition::and);
    }
}
