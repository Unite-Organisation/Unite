package com.app.prod.utils.filters;

import org.jooq.Condition;
import org.jooq.TableRecord;

import java.util.List;

public interface PredicateFilter {
    Condition parseFilterAnd();
    Condition parseFilterOr();
    List<Condition> combineConditions();
}
