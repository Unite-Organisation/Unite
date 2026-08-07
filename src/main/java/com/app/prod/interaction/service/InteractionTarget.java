package com.app.prod.interaction.service;

import com.app.prod.interaction.enums.InteractionEntityType;
import com.app.prod.interaction.enums.InteractionType;

import java.util.UUID;

public interface InteractionTarget {

    InteractionEntityType entityType();

    /**
     * Throws when the entity does not exist or is not visible to the user.
     */
    void assertVisible(UUID userId, UUID entityId);

    /**
     * Throws when the entity is not visible, or when this concrete entity does not accept
     * the given interaction type.
     */
    void assertInteractionAllowed(UUID userId, UUID entityId, InteractionType interactionType);
}
