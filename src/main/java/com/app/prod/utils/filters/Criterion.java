package com.app.prod.utils.filters;

import org.jooq.Condition;

import java.util.Optional;

@FunctionalInterface
public interface Criterion {
    Optional<Condition> toCondition();
}
