package com.app.prod.issues.repository.strategy;

import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static org.jooq.sources.Tables.*;

@Component
public class AreaJoiningStrategy implements IssueJoiningStrategy{
    @Override
    public SelectConditionStep<?> joinEntity(SelectJoinStep<?> step, UUID areaId) {
        return step
                .leftJoin(AREA).on(ISSUE.AREA_ID.eq(AREA.ID))
                .where(AREA.ID.eq(areaId));
    }
}
