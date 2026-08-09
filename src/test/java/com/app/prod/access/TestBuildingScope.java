package com.app.prod.access;

import com.app.prod.user.enums.UserRole;

import java.util.UUID;

public final class TestBuildingScope {

    private TestBuildingScope() {
    }

    public static BuildingScope of(UUID buildingId, UUID userId) {
        return of(buildingId, userId, UserRole.RESIDENT);
    }

    public static BuildingScope of(UUID buildingId, UUID userId, UserRole role) {
        return new BuildingScope(buildingId, userId, role);
    }
}
