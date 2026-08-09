package com.app.prod.facilities;

import com.app.prod.access.BuildingScope;
import com.app.prod.access.TestBuildingScope;
import com.app.prod.builders.AreaPersistenceFactory;
import com.app.prod.builders.BuildingPersistenceFactory;
import com.app.prod.builders.FacilityPersistanceFactory;
import com.app.prod.builders.UserPersistanceFactory;
import com.app.prod.config.IntegrationTest;
import com.app.prod.facilities.dto.FacilityFilterRequest;
import com.app.prod.facilities.dto.FacilityResponse;
import com.app.prod.facilities.enums.FacilityType;
import com.app.prod.facilities.service.FacilityFilteringService;
import com.app.prod.facilities.service.FacilityService;
import com.app.prod.utils.filters.ComparisonFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static com.app.prod.facilities.enums.FacilityType.RECREATION;
import static com.app.prod.facilities.enums.FacilityType.STORAGE;
import static com.app.prod.facilities.enums.FacilityType.WELLNESS;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class FacilityFilteringIT extends IntegrationTest {

    @Autowired
    private UserPersistanceFactory userPersistanceFactory;
    @Autowired
    private AreaPersistenceFactory areaPersistenceFactory;
    @Autowired
    private BuildingPersistenceFactory buildingPersistenceFactory;
    @Autowired
    private FacilityPersistanceFactory facilityPersistanceFactory;

    @Autowired
    private FacilityFilteringService facilityFilteringService;
    @Autowired
    private FacilityService facilityService;

    private UUID buildingId;
    private UUID otherBuildingId;
    private BuildingScope scope;

    @BeforeEach
    void setUp() {
        var area = areaPersistenceFactory.getNewArea().withRandomValues().buildAndSave();

        buildingId = buildingPersistenceFactory.getNewBuilding().withRandomValues().areaId(area.getId()).buildAndSave().getId();
        otherBuildingId = buildingPersistenceFactory.getNewBuilding().withRandomValues().areaId(area.getId()).buildAndSave().getId();

        UUID residentId = userPersistanceFactory.getNewUser().withRandomValues().buildingId(buildingId).buildAndSave().getId();
        scope = TestBuildingScope.of(buildingId, residentId);
    }

    @Test
    void shouldReturnOnlyFacilitiesOfTheBuildingInScope() {
        facility("Pool", RECREATION, buildingId);
        facility("Storage of another building", STORAGE, otherBuildingId);

        assertThat(namesFor(new FacilityFilterRequest())).containsExactly("Pool");
    }

    @Test
    void shouldNarrowResultsToRequestedFacilityType() {
        facility("Gym", RECREATION, buildingId);
        facility("Sauna", WELLNESS, buildingId);

        var request = FacilityFilterRequest.builder().facilityType(WELLNESS).build();

        assertThat(namesFor(request)).containsExactly("Sauna");
    }

    @Test
    void shouldNarrowResultsToFacilitiesRequiringApproval() {
        facility("Party room", RECREATION, buildingId, 20, true);
        facility("Bike room", STORAGE, buildingId, 20, false);

        var request = FacilityFilterRequest.builder().requiresApproval(true).build();

        assertThat(namesFor(request)).containsExactly("Party room");
    }

    @Test
    void shouldCompareCapacityWithGivenModifier() {
        facility("Small room", RECREATION, buildingId, 4, false);
        facility("Big room", RECREATION, buildingId, 40, false);

        var request = FacilityFilterRequest.builder()
                .capacity(10)
                .capacityModifier(ComparisonFilter.Modifier.GREATER_OR_EQUAL_THAN)
                .build();

        assertThat(namesFor(request)).containsExactly("Big room");
    }

    /* Pagination is optional - an absent page falls back to the defaults of FilterRequest. */
    @Test
    void shouldPaginateAndFallBackToDefaultsWhenPageIsNotGiven() {
        facility("A", RECREATION, buildingId);
        facility("B", RECREATION, buildingId);
        facility("C", RECREATION, buildingId);

        assertThat(namesFor(new FacilityFilterRequest())).containsExactly("A", "B", "C");

        var secondPage = FacilityFilterRequest.builder().page(2).pageSize(5).build();
        assertThat(namesFor(secondPage)).isEmpty();
    }

    private void facility(String name, FacilityType type, UUID facilityBuildingId) {
        facility(name, type, facilityBuildingId, 10, false);
    }

    private void facility(String name, FacilityType type, UUID facilityBuildingId, Integer capacity, Boolean requiresApproval) {
        facilityPersistanceFactory.getNewFacility()
                .withRandomValues()
                .name(name)
                .type(type)
                .capacity(capacity)
                .requiresApproval(requiresApproval)
                .buildingId(facilityBuildingId)
                .buildAndSave();
    }

    private List<String> namesFor(FacilityFilterRequest request) {
        var filter = facilityFilteringService.prepareFilter(scope, request);
        return facilityService.getFacilities(request.pagination(), filter).stream()
                .map(FacilityResponse::name)
                .toList();
    }
}
