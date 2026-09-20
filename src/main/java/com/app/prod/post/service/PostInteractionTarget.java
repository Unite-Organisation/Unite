package com.app.prod.post.service;

import com.app.prod.access.BuildingScope;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.interaction.enums.InteractionEntityType;
import com.app.prod.interaction.enums.InteractionType;
import com.app.prod.interaction.service.InteractionTarget;
import com.app.prod.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PostInteractionTarget implements InteractionTarget {

    private final PostRepository postRepository;

    @Override
    public InteractionEntityType entityType() {
        return InteractionEntityType.POST;
    }

    @Override
    public void assertVisible(BuildingScope scope, UUID entityId) {
        postRepository.findPostTypeInBuilding(scope.buildingId(), entityId)
                .orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.POST_NOT_FOUND)));
    }

    @Override
    public void assertInteractionAllowed(BuildingScope scope, UUID entityId, InteractionType interactionType) {
        // attendance is event membership (event_member), no longer an interaction
        if (interactionType == InteractionType.ATTENDING) {
            throw new BadRequestException(AppError.of(
                    Code.INTERACTION_NOT_SUPPORTED,
                    String.format("%s is managed by event membership", InteractionType.ATTENDING)
            ));
        }

        assertVisible(scope, entityId);
    }
}
