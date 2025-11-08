package com.app.prod.utils.filters;

import org.jooq.Condition;
import org.jooq.TableRecord;
import org.jooq.impl.DSL;

import java.util.List;

public interface PredicateFilter {
    List<Condition> combineConditions();

    default Condition parseFilterAnd() {
        return combineConditions().stream().reduce(DSL.trueCondition(), Condition::and);
    }

    default Condition parseFilterOr() {
        return combineConditions().stream().reduce(DSL.trueCondition(), Condition::or);
    }
}
