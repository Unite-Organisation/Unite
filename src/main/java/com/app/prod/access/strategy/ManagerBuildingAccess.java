package com.app.prod.access.strategy;

import com.app.prod.building.repository.BuildingsManagersRepository;
import com.app.prod.user.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.AppUserRecord;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ManagerBuildingAccess implements BuildingAccessStrategy {

    private final BuildingsManagersRepository buildingsManagersRepository;

    @Override
    public UserRole role() {
        return UserRole.MANAGER;
    }

    @Override
    public boolean hasAccess(AppUserRecord user, UUID buildingId) {
        return buildingsManagersRepository.managerManagesBuilding(buildingId, user.getId());
    }
}
