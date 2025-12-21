package com.app.prod.issues.repository;

import com.app.prod.issues.dto.IssueResponse;
import com.app.prod.issues.dto.IssueSimpleResponse;
import com.app.prod.issues.enums.IssuePriority;
import com.app.prod.issues.enums.IssueProcessingStatus;
import com.app.prod.issues.repository.strategy.IssueJoiningStrategy;
import com.app.prod.user.enums.UserRole;
import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.jooq.SelectOnConditionStep;
import org.jooq.sources.tables.Issue;
import org.jooq.sources.tables.records.IssueRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static org.jooq.sources.Tables.*;

@Repository
public class IssueRepository extends BaseJooqRepository<Issue, IssueRecord, UUID> {
    protected IssueRepository(DSLContext dsl) {
        super(dsl, ISSUE, ISSUE.ID);
    }

    public List<IssueResponse> getEntityIssues(UUID entityId, IssueJoiningStrategy joiningStrategy) {
        return null;
    }

    public void updateStatus(UUID issueId, IssueProcessingStatus updatedStatus) {
        dslContext.update(ISSUE)
                .set(ISSUE.STATUS, updatedStatus.name())
                .where(ISSUE.ID.eq(issueId))
                .execute();

    }

    public List<IssueSimpleResponse> getIssues(UUID id) {
        return dslContext.select(
                ISSUE.ID,
                ISSUE.TITLE,
                ISSUE.DESCRIPTION,
                ISSUE.STATUS,
                ISSUE.PRIORITY
        )
                .from(ISSUE)
                .where(ISSUE.CREATED_BY.eq(id))
                .fetch(record -> new IssueSimpleResponse(
                        record.get(ISSUE.ID),
                        record.get(ISSUE.TITLE),
                        record.get(ISSUE.DESCRIPTION),
                        IssueProcessingStatus.valueOf(record.get(ISSUE.STATUS)),
                        IssuePriority.valueOf(record.get(ISSUE.PRIORITY))
                ));
    }
}
