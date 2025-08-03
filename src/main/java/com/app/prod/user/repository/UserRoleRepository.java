package com.app.prod.user.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.UserRoles;
import org.jooq.sources.tables.records.UserRolesRecord;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRoleRepository extends BaseJooqRepository<UserRoles, UserRolesRecord, UUID> {
    protected UserRoleRepository(DSLContext dsl) {
        super(dsl, UserRoles.USER_ROLES, UserRoles.USER_ROLES.ID);
    }

    public Optional<UserRolesRecord> findById(UUID id){
        return dslContext.selectFrom(table)
                .where(table.ID.eq(id))
                .fetchOptional();
    }
}
