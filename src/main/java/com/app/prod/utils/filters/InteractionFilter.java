package com.app.prod.utils.filters;

import com.app.prod.interaction.enums.InteractionEntityType;
import com.app.prod.interaction.enums.InteractionType;
import lombok.Builder;
import org.jooq.Condition;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.app.prod.utils.filters.Criteria.match;
import static com.app.prod.utils.filters.Criteria.matchEnum;
import static org.jooq.sources.Tables.USER_INTERACTION;

@Builder
public class InteractionFilter implements PredicateFilter {
    Filter<InteractionEntityType> entityType;
    Filter<UUID> entityId;
    Filter<InteractionType> interactionType;
    Filter<LocalDateTime> createdAt;

    @Override
    public List<Condition> combineConditions() {
        return Criteria.of(
                matchEnum(USER_INTERACTION.ENTITY_TYPE, entityType),
                match(USER_INTERACTION.ENTITY_ID, entityId),
                matchEnum(USER_INTERACTION.INTERACTION_TYPE, interactionType),
                match(USER_INTERACTION.CREATED_AT, createdAt)
        );
    }
}
