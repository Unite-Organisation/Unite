package com.app.prod.issues.repository.strategy;

import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static org.jooq.sources.Tables.*;

@Component
public class FacilityJoiningStrategy implements IssueJoiningStrategy{
    @Override
    public SelectConditionStep<?> joinEntity(SelectJoinStep<?> step, UUID facilityId) {
        return step
                .leftJoin(FACILITY).on(ISSUE.FACILITY_ID.eq(FACILITY.ID))
                .where(FACILITY.ID.eq(facilityId));
    }
}
