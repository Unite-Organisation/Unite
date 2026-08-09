package com.app.prod.access.strategy;

import com.app.prod.user.enums.UserRole;
import org.jooq.sources.tables.records.AppUserRecord;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ResidentBuildingAccess implements BuildingAccessStrategy {

    @Override
    public UserRole role() {
        return UserRole.RESIDENT;
    }

    @Override
    public boolean hasAccess(AppUserRecord user, UUID buildingId) {
        return buildingId.equals(user.getBuildingId());
    }
}
