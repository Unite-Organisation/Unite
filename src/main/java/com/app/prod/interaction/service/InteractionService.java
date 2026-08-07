package com.app.prod.interaction.service;

import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.interaction.dto.InteractionRequest;
import com.app.prod.interaction.dto.InteractionUserResponse;
import com.app.prod.interaction.enums.InteractionEntityType;
import com.app.prod.interaction.enums.InteractionType;
import com.app.prod.interaction.repository.InteractionRepository;
import com.app.prod.utils.Pagination;
import com.app.prod.utils.filters.InteractionFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class InteractionService {

    private final InteractionRepository interactionRepository;
    private final Clock clock;
    private final Map<InteractionEntityType, InteractionTarget> targets;

    public InteractionService(InteractionRepository interactionRepository, Clock clock, List<InteractionTarget> targets) {
        this.interactionRepository = interactionRepository;
        this.clock = clock;
        this.targets = new EnumMap<>(InteractionEntityType.class);
        targets.forEach(target -> this.targets.put(target.entityType(), target));
    }

    @Transactional
    public void addInteraction(UUID userId, InteractionRequest request) {
        target(request.entityType()).assertInteractionAllowed(userId, request.entityId(), request.interactionType());

        boolean added = interactionRepository.add(
                userId,
                request.entityType(),
                request.entityId(),
                request.interactionType(),
                LocalDateTime.now(clock)
        );

        log.info("Interaction {} on {} {} by user {} - added: {}", request.interactionType(), request.entityType(), request.entityId(), userId, added);
    }

    public void removeInteraction(UUID userId, InteractionEntityType entityType, UUID entityId, InteractionType interactionType) {
        interactionRepository.remove(userId, entityType, entityId, interactionType);
    }

    public List<InteractionUserResponse> getInteractions(UUID userId, InteractionEntityType entityType, UUID entityId, InteractionFilter filter, Pagination pagination) {
        target(entityType).assertVisible(userId, entityId);
        return interactionRepository.findUsers(filter, pagination);
    }

    private InteractionTarget target(InteractionEntityType entityType) {
        InteractionTarget target = targets.get(entityType);
        if (target == null) {
            throw new BadRequestException(AppError.of(Code.INTERACTION_NOT_SUPPORTED, String.format("%s does not support interactions", entityType)));
        }
        return target;
    }
}
