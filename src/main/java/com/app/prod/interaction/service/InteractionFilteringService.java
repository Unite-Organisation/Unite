package com.app.prod.interaction.service;

import com.app.prod.interaction.enums.InteractionEntityType;
import com.app.prod.interaction.enums.InteractionType;
import com.app.prod.utils.filters.ComparisonFilter;
import com.app.prod.utils.filters.InteractionFilter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
public class InteractionFilteringService {

    public InteractionFilter prepareFilter(
            InteractionEntityType entityType,
            UUID entityId,
            InteractionType interactionType,
            LocalDateTime createdAt, ComparisonFilter.Modifier createdAtModifier
    ) {
        return InteractionFilter.builder()
                .entityType(Optional.ofNullable(entityType))
                .entityId(Optional.ofNullable(entityId))
                .interactionType(Optional.ofNullable(interactionType))
                .createdAt(ComparisonFilter.of(createdAt, createdAtModifier))
                .build();
    }
}
