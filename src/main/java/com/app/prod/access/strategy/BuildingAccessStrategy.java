package com.app.prod.access.strategy;

import com.app.prod.user.enums.UserRole;
import org.jooq.sources.tables.records.AppUserRecord;

import java.util.UUID;

public interface BuildingAccessStrategy {

    UserRole role();

    boolean hasAccess(AppUserRecord user, UUID buildingId);
}
