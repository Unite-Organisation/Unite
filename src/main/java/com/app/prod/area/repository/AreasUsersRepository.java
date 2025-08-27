package com.app.prod.area.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.TableField;
import org.jooq.sources.tables.AreasUsers;
import org.jooq.sources.tables.records.AreasUsersRecord;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class AreasUsersRepository extends BaseJooqRepository<AreasUsers, AreasUsersRecord, UUID> {
    protected AreasUsersRepository(DSLContext dsl) {
        super(dsl, AreasUsers.AREAS_USERS, AreasUsers.AREAS_USERS.ID);
    }
}
