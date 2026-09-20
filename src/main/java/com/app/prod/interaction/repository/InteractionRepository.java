package com.app.prod.interaction.repository;

import com.app.prod.interaction.dto.InteractionUserResponse;
import com.app.prod.interaction.enums.InteractionEntityType;
import com.app.prod.interaction.enums.InteractionType;
import com.app.prod.user.dto.UserData;
import com.app.prod.utils.BaseJooqRepository;
import com.app.prod.utils.Pagination;
import com.app.prod.utils.filters.InteractionFilter;
import org.jooq.DSLContext;
import org.jooq.sources.tables.UserInteraction;
import org.jooq.sources.tables.records.UserInteractionRecord;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.sources.Tables.APP_USER;
import static org.jooq.sources.Tables.USER_INTERACTION;

@Repository
public class InteractionRepository extends BaseJooqRepository<UserInteraction, UserInteractionRecord, UUID> {

    protected InteractionRepository(DSLContext dsl) {
        super(dsl, USER_INTERACTION, USER_INTERACTION.ID);
    }

    public boolean add(UUID userId, InteractionEntityType entityType, UUID entityId, InteractionType interactionType, LocalDateTime now) {
        return dslContext.insertInto(USER_INTERACTION)
                .set(USER_INTERACTION.ID, UUID.randomUUID())
                .set(USER_INTERACTION.CREATED_AT, now)
                .set(USER_INTERACTION.USER_ID, userId)
                .set(USER_INTERACTION.ENTITY_TYPE, entityType.name())
                .set(USER_INTERACTION.ENTITY_ID, entityId)
                .set(USER_INTERACTION.INTERACTION_TYPE, interactionType.name())
                .onConflictDoNothing()
                .execute() > 0;
    }

    public boolean remove(UUID userId, InteractionEntityType entityType, UUID entityId, InteractionType interactionType) {
        return dslContext.deleteFrom(USER_INTERACTION)
                .where(USER_INTERACTION.USER_ID.eq(userId))
                .and(USER_INTERACTION.ENTITY_TYPE.eq(entityType.name()))
                .and(USER_INTERACTION.ENTITY_ID.eq(entityId))
                .and(USER_INTERACTION.INTERACTION_TYPE.eq(interactionType.name()))
                .execute() > 0;
    }

    public List<InteractionUserResponse> findUsers(InteractionFilter filter, Pagination pagination) {
        return dslContext.select(
                        USER_INTERACTION.INTERACTION_TYPE,
                        USER_INTERACTION.CREATED_AT,
                        APP_USER.ID,
                        APP_USER.FIRST_NAME,
                        APP_USER.LAST_NAME
                )
                .from(USER_INTERACTION)
                .join(APP_USER).on(APP_USER.ID.eq(USER_INTERACTION.USER_ID))
                .where(filter.parseFilter())
                .orderBy(USER_INTERACTION.CREATED_AT, USER_INTERACTION.ID)
                .offset(pagination.getOffset())
                .limit(pagination.pageSize())
                .fetch(record -> new InteractionUserResponse(
                        new UserData(
                                record.get(APP_USER.ID),
                                record.get(APP_USER.FIRST_NAME),
                                record.get(APP_USER.LAST_NAME)
                        ),
                        InteractionType.valueOf(record.get(USER_INTERACTION.INTERACTION_TYPE)),
                        record.get(USER_INTERACTION.CREATED_AT)
                ));
    }

    public int countInteractions(InteractionEntityType entityType, UUID entityId, InteractionType interactionType) {
        return Optional.ofNullable(
                dslContext.selectCount()
                        .from(USER_INTERACTION)
                        .where(USER_INTERACTION.ENTITY_TYPE.eq(entityType.name()))
                        .and(USER_INTERACTION.ENTITY_ID.eq(entityId))
                        .and(USER_INTERACTION.INTERACTION_TYPE.eq(interactionType.name()))
                        .fetchOneInto(Integer.class)
        ).orElse(0);
    }
}
