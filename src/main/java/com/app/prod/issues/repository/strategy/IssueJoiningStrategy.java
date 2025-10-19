package com.app.prod.issues.repository.strategy;

import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;

import java.util.UUID;

public interface IssueJoiningStrategy {
    SelectConditionStep<?> joinEntity(SelectJoinStep<?> step, UUID entityId);
}
