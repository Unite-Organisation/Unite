package com.app.prod.utils.filters;

import com.app.prod.interaction.enums.InteractionEntityType;
import com.app.prod.interaction.enums.InteractionType;
import lombok.Builder;
import org.jooq.Condition;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.sources.Tables.USER_INTERACTION;

@Builder
public class InteractionFilter implements PredicateFilter {
    Optional<InteractionEntityType> entityType;
    Optional<UUID> entityId;
    Optional<InteractionType> interactionType;
    ComparisonFilter<LocalDateTime> createdAt;

    @Override
    public List<Condition> combineConditions() {
        List<Condition> conditionList = new ArrayList<>();

        entityType.ifPresent(r -> conditionList.add(USER_INTERACTION.ENTITY_TYPE.eq(r.name())));
        entityId.ifPresent(r -> conditionList.add(USER_INTERACTION.ENTITY_ID.eq(r)));
        interactionType.ifPresent(r -> conditionList.add(USER_INTERACTION.INTERACTION_TYPE.eq(r.name())));
        createdAt.toCondition(USER_INTERACTION.CREATED_AT).ifPresent(conditionList::add);

        return conditionList;
    }
}
