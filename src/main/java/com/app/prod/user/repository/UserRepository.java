package com.app.prod.user.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.Users;
import org.jooq.sources.tables.records.UsersRecord;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class UserRepository extends BaseJooqRepository<Users, UsersRecord, UUID> {
    protected UserRepository(DSLContext dsl) {
        super(dsl, Users.USERS, Users.USERS.ID);
    }
}
