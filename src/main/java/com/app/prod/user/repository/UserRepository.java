package com.app.prod.user.repository;

import com.app.prod.user.dto.BasicUserData;
import com.app.prod.user.dto.PotentialContactResponse;
import com.app.prod.user.enums.UserStatus;
import com.app.prod.utils.BaseJooqRepository;
import com.app.prod.utils.Pagination;
import org.jooq.DSLContext;
import org.jooq.sources.tables.AppUser;
import org.jooq.sources.tables.records.AppUserRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.app.prod.user.enums.UserStatus.ACTIVE;
import static org.jooq.sources.Tables.*;

@Repository
public class UserRepository extends BaseJooqRepository<AppUser, AppUserRecord, UUID> {
    protected UserRepository(DSLContext dsl) {
        super(dsl, APP_USER, APP_USER.ID);
    }

    public Optional<AppUserRecord> findByUsername(String username){
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
                .set(table.STATUS, ACTIVE.name())
                .where(table.USERNAME.eq(temporaryUserName))
                .execute();
    }

    public void addBuilding(UUID userId, UUID buildingId){
        dslContext.update(table)
                .set(table.BUILDING_ID, buildingId)
                .where(table.ID.eq(userId))
                .execute();
    }

    public List<PotentialContactResponse> getAllUsersInArea(UUID areaId, Pagination pagination) {
        return dslContext.select(
                APP_USER.ID,
                APP_USER.FIRST_NAME,
                APP_USER.LAST_NAME,
                USER_ROLE.USER_ROLE_,
                BUILDING.ID,
                BUILDING.NAME
        )
                .from(APP_USER)
                .leftJoin(BUILDING).on(APP_USER.BUILDING_ID.eq(BUILDING.ID))
                .leftJoin(AREA).on(BUILDING.AREA_ID.eq(areaId))
                .leftJoin(USER_ROLE).on(APP_USER.USER_ROLE.eq(USER_ROLE.ID))
                .where(AREA.ID.eq(areaId))
                .and(APP_USER.STATUS.eq(ACTIVE.name()))
                .offset(pagination.getOffset())
                .limit(pagination.pageSize())
                .fetch(record -> {
                    BasicUserData basicUserData = new BasicUserData(
                            record.get(APP_USER.ID),
                            record.get(APP_USER.FIRST_NAME),
                            record.get(APP_USER.LAST_NAME),
                            record.get(USER_ROLE.USER_ROLE_)
                    );

                    return new PotentialContactResponse(
                      basicUserData,
                      record.get(BUILDING.ID),
                      record.get(BUILDING.NAME)
                    );
                });
    }
}
