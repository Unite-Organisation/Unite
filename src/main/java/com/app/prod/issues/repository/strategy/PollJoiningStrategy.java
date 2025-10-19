package com.app.prod.issues.repository.strategy;

import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static org.jooq.sources.Tables.*;

@Component
public class PollJoiningStrategy implements IssueJoiningStrategy{
    @Override
    public SelectConditionStep<?> joinEntity(SelectJoinStep<?> step, UUID pollId) {
        return step
                .leftJoin(POLLS).on(ISSUE.POLL_ID.eq(POLLS.ID))
                .where(POLLS.ID.eq(pollId));
    }
}