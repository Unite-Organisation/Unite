package com.app.prod.access;

import com.app.prod.builders.AreaPersistenceFactory;
import com.app.prod.builders.BuildingPersistenceFactory;
import com.app.prod.builders.BuildingsManagersPersistenceFactory;
import com.app.prod.builders.UserPersistanceFactory;
import com.app.prod.config.IntegrationTest;
import com.app.prod.exceptions.exceptions.UnauthorizedDataAccessException;
import com.app.prod.user.enums.UserRole;
import org.jooq.sources.tables.records.AppUserRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
public class BuildingAccessIT extends IntegrationTest {

    @Autowired
    private UserPersistanceFactory userPersistanceFactory;
    @Autowired
    private AreaPersistenceFactory areaPersistenceFactory;
    @Autowired
    private BuildingPersistenceFactory buildingPersistenceFactory;
    @Autowired
    private BuildingsManagersPersistenceFactory buildingsManagersPersistenceFactory;
    @Autowired
    private BuildingAccessService buildingAccessService;

    private UUID buildingId;
    private UUID otherBuildingId;

    @BeforeEach
    void setUp() {
        var area = areaPersistenceFactory.getNewArea().withRandomValues().buildAndSave();

        buildingId = buildingPersistenceFactory.getNewBuilding().withRandomValues().areaId(area.getId()).buildAndSave().getId();
        otherBuildingId = buildingPersistenceFactory.getNewBuilding().withRandomValues().areaId(area.getId()).buildAndSave().getId();
    }

    @Test
    void shouldLetResidentIntoTheirOwnBuilding() {
        var resident = resident(buildingId);

        var scope = buildingAccessService.authorize(resident, UserRole.RESIDENT, buildingId);

        assertThat(scope.buildingId()).isEqualTo(buildingId);
        assertThat(scope.userId()).isEqualTo(resident.getId());
        assertThat(scope.role()).isEqualTo(UserRole.RESIDENT);
    }

    @Test
    void shouldRejectResidentInAnotherBuildingOfTheSameArea() {
        var resident = resident(buildingId);

        assertThatThrownBy(() -> buildingAccessService.authorize(resident, UserRole.RESIDENT, otherBuildingId))
                .isInstanceOf(UnauthorizedDataAccessException.class);
    }

    @Test
    void shouldRejectResidentWithoutAnyBuilding() {
        var resident = userPersistanceFactory.getNewUser().withRandomValues().buildAndSave();

        assertThatThrownBy(() -> buildingAccessService.authorize(resident, UserRole.RESIDENT, buildingId))
                .isInstanceOf(UnauthorizedDataAccessException.class);
    }

    @Test
    void shouldLetManagerIntoEveryBuildingTheyManage() {
        var manager = manager();
        assignManager(manager.getId(), buildingId);
        assignManager(manager.getId(), otherBuildingId);

        assertThat(buildingAccessService.authorize(manager, UserRole.MANAGER, buildingId).buildingId()).isEqualTo(buildingId);
        assertThat(buildingAccessService.authorize(manager, UserRole.MANAGER, otherBuildingId).buildingId()).isEqualTo(otherBuildingId);
    }

    @Test
    void shouldRejectManagerInBuildingTheyDoNotManage() {
        var manager = manager();
        assignManager(manager.getId(), buildingId);

        assertThatThrownBy(() -> buildingAccessService.authorize(manager, UserRole.MANAGER, otherBuildingId))
                .isInstanceOf(UnauthorizedDataAccessException.class);
    }

    @Test
    void shouldRejectUnknownBuilding() {
        var resident = resident(buildingId);

        assertThatThrownBy(() -> buildingAccessService.authorize(resident, UserRole.RESIDENT, UUID.randomUUID()))
                .isInstanceOf(UnauthorizedDataAccessException.class);
    }

    @Test
    void shouldLetAdminIntoAnyExistingBuildingOnly() {
        var admin = userPersistanceFactory.getNewUser().withRandomValues().userRole(UserRole.ADMIN).buildAndSave();

        assertThat(buildingAccessService.authorize(admin, UserRole.ADMIN, otherBuildingId).buildingId()).isEqualTo(otherBuildingId);
        assertThatThrownBy(() -> buildingAccessService.authorize(admin, UserRole.ADMIN, UUID.randomUUID()))
                .isInstanceOf(UnauthorizedDataAccessException.class);
    }

    @Test
    void shouldRejectUnauthenticatedCaller() {
        assertThatThrownBy(() -> buildingAccessService.authorize(null, null, buildingId))
                .isInstanceOf(UnauthorizedDataAccessException.class);
    }

    private AppUserRecord resident(UUID residentBuildingId) {
        return userPersistanceFactory.getNewUser().withRandomValues().buildingId(residentBuildingId).buildAndSave();
    }

    private AppUserRecord manager() {
        return userPersistanceFactory.getNewUser().withRandomValues().userRole(UserRole.MANAGER).buildAndSave();
    }

    private void assignManager(UUID managerId, UUID managedBuildingId) {
        buildingsManagersPersistenceFactory.addNewBuildingManager()
                .buildingId(managedBuildingId)
                .managerId(managerId)
                .buildAndSave();
    }
}
