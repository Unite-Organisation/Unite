package com.app.prod.issues.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.Issue;
import org.jooq.sources.tables.records.IssueRecord;

import java.util.UUID;

import static org.jooq.sources.Tables.ISSUE;

public class IssueRepository extends BaseJooqRepository<Issue, IssueRecord, UUID> {
    protected IssueRepository(DSLContext dsl) {
        super(dsl, ISSUE, ISSUE.ID);
    }
}
