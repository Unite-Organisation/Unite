package com.app.prod.interaction.repository;

import com.app.prod.interaction.dto.InteractionSummary;
import com.app.prod.interaction.enums.InteractionEntityType;
import com.app.prod.interaction.enums.InteractionType;
import com.app.prod.utils.filters.Related;
import org.jooq.Condition;
import org.jooq.Field;

import java.util.List;
import java.util.UUID;

import static org.jooq.impl.DSL.*;
import static org.jooq.sources.Tables.USER_INTERACTION;

public class InteractionFields {

    public static Field<List<InteractionSummary>> summaryFor(Field<UUID> entityId, InteractionEntityType entityType, UUID userId) {
        Field<Integer> interactionCount = count().as("interaction_count");
        Field<Boolean> reactedByCurrentUser = boolOr(USER_INTERACTION.USER_ID.eq(userId)).as("reacted_by_current_user");

        return multiset(
                select(USER_INTERACTION.INTERACTION_TYPE, interactionCount, reactedByCurrentUser)
                        .from(USER_INTERACTION)
                        .where(USER_INTERACTION.ENTITY_TYPE.eq(entityType.name()))
                        .and(USER_INTERACTION.ENTITY_ID.eq(entityId))
                        .groupBy(USER_INTERACTION.INTERACTION_TYPE)
        ).as("interactions")
                .convertFrom(result -> result.map(record -> new InteractionSummary(
                        InteractionType.valueOf(record.get(USER_INTERACTION.INTERACTION_TYPE)),
                        record.get(interactionCount),
                        record.get(reactedByCurrentUser)
                )));
    }

    public static Condition reactedBy(Field<UUID> entityId, InteractionEntityType entityType, InteractionType interactionType, UUID userId) {
        return Related.existsIn(USER_INTERACTION,
                USER_INTERACTION.ENTITY_ID.eq(entityId),
                USER_INTERACTION.ENTITY_TYPE.eq(entityType.name()),
                USER_INTERACTION.INTERACTION_TYPE.eq(interactionType.name()),
                USER_INTERACTION.USER_ID.eq(userId));
    }
}
