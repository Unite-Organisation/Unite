package com.app.prod.access;

import com.app.prod.access.strategy.BuildingAccessStrategy;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.IllegalApplicationStateException;
import com.app.prod.exceptions.exceptions.UnauthorizedDataAccessException;
import com.app.prod.user.enums.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.AppUserRecord;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class BuildingAccessService {

    private final Map<UserRole, BuildingAccessStrategy> strategies;

    public BuildingAccessService(List<BuildingAccessStrategy> strategies) {
        this.strategies = new EnumMap<>(UserRole.class);
        strategies.forEach(strategy -> this.strategies.put(strategy.role(), strategy));
    }

    public BuildingScope authorize(AppUserRecord user, UserRole role, UUID buildingId) {
        if (user == null || role == null) {
            throw new UnauthorizedDataAccessException(AppError.of(Code.ACCESS_DENIED, "Current user could not be resolved"));
        }

        if (!strategy(role).hasAccess(user, buildingId)) {
            log.info("User {} with role {} was denied access to building {}", user.getId(), role, buildingId);
            throw new UnauthorizedDataAccessException(AppError.of(
                    Code.BUILDING_ACCESS_DENIED,
                    String.format("User %s does not have access to building %s", user.getId(), buildingId)
            ));
        }

        return new BuildingScope(buildingId, user.getId(), role);
    }

    private BuildingAccessStrategy strategy(UserRole role) {
        BuildingAccessStrategy strategy = strategies.get(role);
        if (strategy == null) {
            throw new IllegalApplicationStateException(AppError.of(
                    Code.BUILDING_ACCESS_DENIED,
                    String.format("No building access rule defined for role %s", role)
            ));
        }
        return strategy;
    }
}
