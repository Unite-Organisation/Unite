package com.app.prod.user.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.Users;
import org.jooq.sources.tables.records.UsersRecord;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository extends BaseJooqRepository<Users, UsersRecord> {
    protected UserRepository(DSLContext dsl) {
        super(dsl, Users.USERS);
    }
}
