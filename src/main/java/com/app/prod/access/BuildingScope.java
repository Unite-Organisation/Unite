package com.app.prod.access;

import com.app.prod.user.enums.UserRole;

import java.util.UUID;

public final class BuildingScope {

    private final UUID buildingId;
    private final UUID userId;
    private final UserRole role;

    BuildingScope(UUID buildingId, UUID userId, UserRole role) {
        this.buildingId = buildingId;
        this.userId = userId;
        this.role = role;
    }

    public UUID buildingId() {
        return buildingId;
    }

    public UUID userId() {
        return userId;
    }

    public UserRole role() {
        return role;
    }
}
