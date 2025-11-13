package com.app.prod.user.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.records.UserRoleRecord;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import static org.jooq.sources.Tables.USER_ROLE;

@Repository
public class UserRoleRepository extends BaseJooqRepository<org.jooq.sources.tables.UserRole, UserRoleRecord, UUID> {
    protected UserRoleRepository(DSLContext dsl) {
        super(dsl, USER_ROLE, USER_ROLE.ID);
    }

    public Optional<UserRoleRecord> findByRoleName(com.app.prod.user.enums.UserRole userRole){
        return dslContext.selectFrom(table)
                .where(table.USER_ROLE_.eq(userRole.name()))
                .fetchOptional();
    }
}
