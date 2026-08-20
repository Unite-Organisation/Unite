package com.app.prod.user.repository;

import com.app.prod.area.enums.AreaType;
import com.app.prod.building.dto.HomePageResponse;
import com.app.prod.mail.dto.DeliverySummary;
import com.app.prod.mail.enums.EmailDeliveryStatus;
import com.app.prod.mail.enums.EmailDeliveryType;
import com.app.prod.mail.repository.EmailDeliveryFields;
import com.app.prod.user.dto.BasicUserData;
import com.app.prod.user.dto.BuildingUserResponse;
import com.app.prod.user.dto.ExistingAccount;
import com.app.prod.user.dto.PotentialContactResponse;
import com.app.prod.user.dto.ResidentToAdd;
import com.app.prod.user.dto.UserMetaInfo;
import com.app.prod.user.enums.UserRole;
import com.app.prod.user.enums.UserStatus;
import com.app.prod.utils.BaseJooqRepository;
import com.app.prod.utils.Pagination;
import com.app.prod.utils.filters.BuildingUserFilter;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.sources.tables.AppUser;
import org.jooq.sources.tables.records.AppUserRecord;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
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

    public Optional<AppUserRecord> findByEmail(String email){
        return dslContext.selectFrom(table)
                .where(table.EMAIL.eq(email))
                .fetchOptional();
    }

    public boolean adminExists(UUID adminRoleId) {
        return dslContext.fetchExists(
                dslContext.selectOne()
                        .from(APP_USER)
                        .where(APP_USER.USER_ROLE.eq(adminRoleId))
        );
    }

    public List<ExistingAccount> findAccountsByEmails(Collection<String> emails) {
        Field<String> lastInvitation = EmailDeliveryFields.lastDeliveryStatus(APP_USER.ID, EmailDeliveryType.USER_CREATION);

        return dslContext.select(APP_USER.ID, APP_USER.EMAIL, APP_USER.STATUS, lastInvitation)
                .from(APP_USER)
                .where(DSL.lower(APP_USER.EMAIL).in(emails))
                .fetch(record -> new ExistingAccount(
                        record.get(APP_USER.ID),
                        record.get(APP_USER.EMAIL).toLowerCase(Locale.ROOT),
                        UserStatus.valueOf(record.get(APP_USER.STATUS)),
                        record.get(lastInvitation) == null ? null : EmailDeliveryStatus.valueOf(record.get(lastInvitation))
                ));
    }

    public void activateUser(UUID userId, String username, String password, String firstName, String lastName){
        dslContext.update(table)
                .set(table.USERNAME, username)
                .set(table.PASSWORD, password)
                .set(table.FIRST_NAME, firstName)
                .set(table.LAST_NAME, lastName)
                .set(table.STATUS, ACTIVE.name())
                .where(table.ID.eq(userId))
                .execute();
    }

    public void addBuilding(UUID userId, UUID buildingId){
        dslContext.update(table)
                .set(table.BUILDING_ID, buildingId)
                .where(table.ID.eq(userId))
                .execute();
    }

    public List<PotentialContactResponse> getAllUsersInArea(UUID areaId, UUID userId, Pagination pagination) {
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
                .and(APP_USER.ID.ne(userId))
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

    public List<BuildingUserResponse> findUsersInBuilding(Pagination pagination, BuildingUserFilter filter) {
        Field<DeliverySummary> invitation = EmailDeliveryFields.lastDelivery(APP_USER.ID, EmailDeliveryType.USER_CREATION);

        return dslContext.select(
                        APP_USER.ID,
                        APP_USER.FIRST_NAME,
                        APP_USER.LAST_NAME,
                        APP_USER.USERNAME,
                        APP_USER.EMAIL,
                        APP_USER.STATUS,
                        APP_USER.CREATED_AT,
                        invitation
                )
                .from(APP_USER)
                .where(filter.parseFilter())
                .orderBy(APP_USER.CREATED_AT.desc(), APP_USER.ID)
                .offset(pagination.getOffset())
                .limit(pagination.pageSize())
                .fetch(record -> {
                    UserStatus status = UserStatus.valueOf(record.get(APP_USER.STATUS));

                    return new BuildingUserResponse(
                            record.get(APP_USER.ID),
                            record.get(APP_USER.FIRST_NAME),
                            record.get(APP_USER.LAST_NAME),
                            record.get(APP_USER.USERNAME),
                            record.get(APP_USER.EMAIL),
                            status,
                            record.get(APP_USER.CREATED_AT),
                            status == UserStatus.ACTIVE ? null : record.get(invitation)
                    );
                });
    }

    public HomePageResponse getUsersData(UUID id) {
        return dslContext.select(
                APP_USER.ID,
                APP_USER.USERNAME,
                USER_ROLE.USER_ROLE_,
                APP_USER.STATUS,
                AREA.ID,
                AREA.NAME,
                AREA.TYPE,
                BUILDING.ID,
                BUILDING.NAME,
                BUILDING.COUNTRY,
                BUILDING.STREET,
                BUILDING.NUMBER
        )
                .from(APP_USER)
                .leftJoin(USER_ROLE).on(APP_USER.USER_ROLE.eq(USER_ROLE.ID))
                .leftJoin(BUILDING).on(APP_USER.BUILDING_ID.eq(BUILDING.ID))
                .leftJoin(AREA).on(BUILDING.AREA_ID.eq(AREA.ID))
                .where(APP_USER.ID.eq(id))
                .fetchOne(record -> new HomePageResponse(
                        record.get(APP_USER.ID),
                        record.get(APP_USER.USERNAME),
                        UserRole.valueOf(record.get(USER_ROLE.USER_ROLE_)),
                        UserStatus.valueOf(record.get(APP_USER.STATUS)),
                        record.get(AREA.ID),
                        record.get(AREA.NAME),
                        AreaType.valueOf(record.get(AREA.TYPE)),
                        record.get(BUILDING.ID),
                        record.get(BUILDING.NAME),
                        record.get(BUILDING.COUNTRY),
                        record.get(BUILDING.STREET),
                        record.get(BUILDING.NUMBER)
                ));

    }

    public List<ResidentToAdd> getUsersWithoutBuilding() {
        return dslContext.select(
                APP_USER.FIRST_NAME,
                APP_USER.LAST_NAME,
                APP_USER.ID,
                APP_USER.USERNAME,
                APP_USER.EMAIL
        )
                .from(APP_USER)
                .leftJoin(USER_ROLE).on(USER_ROLE.ID.eq(APP_USER.USER_ROLE))
                .where(APP_USER.BUILDING_ID.isNull())
                .and(USER_ROLE.USER_ROLE_.eq(UserRole.RESIDENT.name()))
                .fetch(record -> new ResidentToAdd(
                        record.get(APP_USER.FIRST_NAME),
                        record.get(APP_USER.LAST_NAME),
                        record.get(APP_USER.ID),
                        record.get(APP_USER.USERNAME),
                        record.get(APP_USER.EMAIL)
                ));
    }

    public List<UUID> getAllUsersInArea(UUID areaId){
        return dslContext.select(APP_USER.ID)
                .from(AREA)
                .leftJoin(BUILDING).on(BUILDING.AREA_ID.eq(AREA.ID))
                .leftJoin(APP_USER).on(APP_USER.BUILDING_ID.eq(BUILDING.ID))
                .where(AREA.ID.eq(areaId))
                .fetchInto(UUID.class);
    }

    public UserMetaInfo getUserMetaData(UUID userId) {
        return dslContext.select(
                APP_USER.ID,
                BUILDING.ID,
                AREA.ID
        )
                .from(APP_USER)
                .leftJoin(BUILDING).on(BUILDING.ID.eq(APP_USER.BUILDING_ID))
                .leftJoin(AREA).on(AREA.ID.eq(BUILDING.AREA_ID))
                .where(APP_USER.ID.eq(userId))
                .fetchOneInto(UserMetaInfo.class);
    }
}
