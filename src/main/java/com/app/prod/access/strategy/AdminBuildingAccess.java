package com.app.prod.access.strategy;

import com.app.prod.building.repository.BuildingRepository;
import com.app.prod.user.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.AppUserRecord;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AdminBuildingAccess implements BuildingAccessStrategy {

    private final BuildingRepository buildingRepository;

    @Override
    public UserRole role() {
        return UserRole.ADMIN;
    }

    @Override
    public boolean hasAccess(AppUserRecord user, UUID buildingId) {
        return buildingRepository.exists(buildingId);
    }
}
