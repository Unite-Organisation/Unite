package com.app.prod.issues.repository.strategy;

import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static org.jooq.sources.Tables.BUILDINGS;
import static org.jooq.sources.Tables.ISSUE;

@Component
public class BuildingJoiningStrategy implements IssueJoiningStrategy{

    @Override
    public SelectConditionStep<?> joinEntity(SelectJoinStep<?> step, UUID buildingId){
        return step
                .leftJoin(BUILDINGS).on(ISSUE.BUILDING_ID.eq(BUILDINGS.ID))
                .where(BUILDINGS.ID.eq(buildingId));
    }
}
