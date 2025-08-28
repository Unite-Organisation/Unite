package com.app.prod.user.repository;

import com.app.prod.user.enums.UserStatus;
import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.Users;
import org.jooq.sources.tables.records.UsersRecord;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepository extends BaseJooqRepository<Users, UsersRecord, UUID> {
    protected UserRepository(DSLContext dsl) {
        super(dsl, Users.USERS, Users.USERS.ID);
    }

    public Optional<UsersRecord> findByUsername(String username){
        return dslContext.selectFrom(table)
                .where(table.USERNAME.eq(username))
                .fetchOptional();
    }

    public boolean temporaryCredentialsAreValid(String login, String password){
        return dslContext.fetchExists(
                dslContext.selectOne()
                        .from(table)
                        .where(table.USERNAME.eq(login).and(table.PASSWORD.eq(password)))
        );
    }

    public void activateUser(String temporaryUserName, String email, String username, String password){
        dslContext.update(table)
                .set(table.USERNAME, username)
                .set(table.PASSWORD, password)
                .set(table.EMAIL, email)
                .set(table.STATUS, UserStatus.ACTIVE.name())
                .where(table.USERNAME.eq(temporaryUserName))
                .execute();
    }
}
