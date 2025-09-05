package com.app.prod.facilities;

import com.app.prod.builders.FacilitiesReservationnPersistanceFactory;
import com.app.prod.builders.FacilityPersistanceFactory;
import com.app.prod.builders.UserPersistanceFactory;
import com.app.prod.config.IntegrationTest;
import com.app.prod.facilities.repository.FacilityReservationsRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.jooq.sources.tables.records.FacilitiesRecord;
import org.jooq.sources.tables.records.UsersRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDateTime;

@SpringBootTest
public class FacilitiesReservationRepositoryIT extends IntegrationTest {

    @Autowired
    private UserPersistanceFactory userPersistanceFactory;
    @Autowired
    private FacilityPersistanceFactory facilityPersistanceFactory;
    @Autowired
    private FacilitiesReservationnPersistanceFactory facilitiesReservationnPersistanceFactory;
    @Autowired
    private FacilityReservationsRepository facilityReservationsRepository;

    private UsersRecord user;
    private FacilitiesRecord facility;

    private void stubUserAndFacility(String username){
        user = userPersistanceFactory.getNewUser()
                .withRandomValues()
                .username(username)
                .buildAndSave();

        facility = facilityPersistanceFactory.getNewFacility()
                .withRandomValues()
                .buildAndSave();
    }

    @Test
    @Operation(summary = "Testing overlapping reservations")
    void shouldReturnTwoOverlappingReservations() {
        stubUserAndFacility("BobTester1");
        var reservation1 = facilitiesReservationnPersistanceFactory.getNewReservation()
                .withRandomValues()
                .facilityId(facility.getId())
                .userId(user.getId())
                .startTime(LocalDateTime.of(2025, 1, 10, 12, 0))
                .endTime(LocalDateTime.of(2025, 1, 10, 14, 0))
                .buildAndSave();

        var reservation2 = facilitiesReservationnPersistanceFactory.getNewReservation()
                .withRandomValues()
                .facilityId(facility.getId())
                .userId(user.getId())
                .startTime(LocalDateTime.of(2025, 1, 10, 15, 0))
                .endTime(LocalDateTime.of(2025, 1, 10, 18, 0))
                .buildAndSave();

        var overlappingReservations = facilityReservationsRepository.getOverlappingReservationsForFacility(
                facility.getId(),
                LocalDateTime.of(2025, 1, 10, 13, 0),
                LocalDateTime.of(2025, 1, 10, 19, 0)
        );

        assertThat(overlappingReservations).hasSize(2);
    }

    @Test
    @Operation(summary = "Testing corner cases (end of reservation and start of another at same time) etc")
    void shouldNotReturnAnyOverlappingFacilities() {
        stubUserAndFacility("BobTester2");
        var reservation = facilitiesReservationnPersistanceFactory.getNewReservation()
                .withRandomValues()
                .facilityId(facility.getId())
                .userId(user.getId())
                .startTime(LocalDateTime.of(2025, 1, 10, 12, 0))
                .endTime(LocalDateTime.of(2025, 1, 10, 14, 0))
                .buildAndSave();

        var overlappingReservations = facilityReservationsRepository.getOverlappingReservationsForFacility(
                facility.getId(),
                LocalDateTime.of(2025, 1, 10, 14, 0),
                LocalDateTime.of(2025, 1, 10, 19, 0)
        );

        assertThat(overlappingReservations).isEmpty();
    }

}
