package com.app.prod.interaction.service;

import com.app.prod.access.BuildingScope;
import com.app.prod.interaction.enums.InteractionEntityType;
import com.app.prod.interaction.enums.InteractionType;

import java.util.UUID;

public interface InteractionTarget {

    InteractionEntityType entityType();

    /**
     * Throws when the entity does not exist or does not belong to the building being operated in.
     */
    void assertVisible(BuildingScope scope, UUID entityId);

    /**
     * Throws when the entity is not visible, or when this concrete entity does not accept
     * the given interaction type.
     */
    void assertInteractionAllowed(BuildingScope scope, UUID entityId, InteractionType interactionType);
}
