package com.app.prod.facilities;

import com.app.prod.builders.AreaPersistenceFactory;
import com.app.prod.builders.BuildingPersistenceFactory;
import com.app.prod.builders.FacilitiesReservationnPersistanceFactory;
import com.app.prod.builders.FacilityPersistanceFactory;
import com.app.prod.builders.UserPersistanceFactory;
import com.app.prod.config.IntegrationTest;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.facilities.dto.FacilityAvailabilityFilterRequest;
import com.app.prod.facilities.dto.FacilitySlot;
import com.app.prod.facilities.enums.ReservationStatus;
import com.app.prod.facilities.service.FacilityFilteringService;
import com.app.prod.facilities.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
public class FacilityAvailabilityIT extends IntegrationTest {

    private static final LocalDate DAY = LocalDate.of(2026, 3, 12);

    @Autowired
    private UserPersistanceFactory userPersistanceFactory;
    @Autowired
    private AreaPersistenceFactory areaPersistenceFactory;
    @Autowired
    private BuildingPersistenceFactory buildingPersistenceFactory;
    @Autowired
    private FacilityPersistanceFactory facilityPersistanceFactory;
    @Autowired
    private FacilitiesReservationnPersistanceFactory reservationPersistanceFactory;

    @Autowired
    private FacilityFilteringService facilityFilteringService;
    @Autowired
    private ReservationService reservationService;

    private UUID facilityId;
    private UUID otherFacilityId;
    private UUID viewerId;
    private UUID neighbourId;

    @BeforeEach
    void setUp() {
        var area = areaPersistenceFactory.getNewArea().withRandomValues().buildAndSave();
        var buildingId = buildingPersistenceFactory.getNewBuilding().withRandomValues().areaId(area.getId()).buildAndSave().getId();

        facilityId = facilityPersistanceFactory.getNewFacility().withRandomValues().buildingId(buildingId).buildAndSave().getId();
        otherFacilityId = facilityPersistanceFactory.getNewFacility().withRandomValues().buildingId(buildingId).buildAndSave().getId();

        viewerId = userPersistanceFactory.getNewUser().withRandomValues().firstName("Anna").lastName("Kowalska").buildingId(buildingId).buildAndSave().getId();
        neighbourId = userPersistanceFactory.getNewUser().withRandomValues().firstName("Piotr").lastName("Nowak").buildingId(buildingId).buildAndSave().getId();
    }

    @Test
    void shouldSplitTheDayIntoTakenAndFreeSlots() {
        reservation(facilityId, neighbourId, at(10, 0), at(12, 0));

        var slots = availability(new FacilityAvailabilityFilterRequest());

        assertThat(slots)
                .extracting(FacilitySlot::startTime, FacilitySlot::endTime, FacilitySlot::taken)
                .containsExactly(
                        tuple(dayStart(), at(10, 0), false),
                        tuple(at(10, 0), at(12, 0), true),
                        tuple(at(12, 0), dayEnd(), false)
                );
    }

    @Test
    void shouldReturnTheWholeDayAsFreeWhenNothingIsReserved() {
        var slots = availability(new FacilityAvailabilityFilterRequest());

        assertThat(slots)
                .extracting(FacilitySlot::startTime, FacilitySlot::endTime, FacilitySlot::taken)
                .containsExactly(tuple(dayStart(), dayEnd(), false));
    }

    @Test
    void shouldTellWhoTookTheSlot() {
        reservation(facilityId, neighbourId, at(10, 0), at(12, 0));

        var request = FacilityAvailabilityFilterRequest.builder().takenOnly(true).build();

        assertThat(availability(request))
                .singleElement()
                .satisfies(slot -> {
                    assertThat(slot.reservation().userFirstName()).isEqualTo("Piotr");
                    assertThat(slot.reservation().userLastName()).isEqualTo("Nowak");
                    assertThat(slot.reservation().status()).isEqualTo(ReservationStatus.RESERVED);
                });
    }

    @Test
    void shouldReturnOnlyFreeSlotsWhenAvailableOnlyIsRequested() {
        reservation(facilityId, neighbourId, at(10, 0), at(12, 0));

        var request = FacilityAvailabilityFilterRequest.builder().availableOnly(true).build();

        assertThat(availability(request))
                .extracting(FacilitySlot::startTime, FacilitySlot::endTime)
                .containsExactly(
                        tuple(dayStart(), at(10, 0)),
                        tuple(at(12, 0), dayEnd())
                );
        assertThat(availability(request)).allMatch(slot -> !slot.taken() && slot.reservation() == null);
    }

    @Test
    void shouldReturnOnlyOwnReservationsWhenMyBookingsOnlyIsRequested() {
        reservation(facilityId, neighbourId, at(10, 0), at(12, 0));
        reservation(facilityId, viewerId, at(14, 0), at(15, 0));

        var request = FacilityAvailabilityFilterRequest.builder().myBookingsOnly(true).build();

        assertThat(availability(request))
                .extracting(FacilitySlot::startTime, FacilitySlot::endTime, FacilitySlot::taken)
                .containsExactly(tuple(at(14, 0), at(15, 0), true));
    }

    @Test
    void shouldIgnoreReservationsOfAnotherDayAndAnotherFacility() {
        reservation(facilityId, neighbourId, DAY.minusDays(1).atTime(10, 0), DAY.minusDays(1).atTime(12, 0));
        reservation(otherFacilityId, neighbourId, at(10, 0), at(12, 0));

        assertThat(availability(new FacilityAvailabilityFilterRequest()))
                .extracting(FacilitySlot::taken)
                .containsExactly(false);
    }

    /* A reservation running over midnight is cut to the requested day instead of leaking into it. */
    @Test
    void shouldClampReservationsCrossingMidnightToTheRequestedDay() {
        reservation(facilityId, neighbourId, DAY.minusDays(1).atTime(23, 0), at(1, 0));
        reservation(facilityId, neighbourId, at(23, 0), DAY.plusDays(1).atTime(1, 0));

        assertThat(availability(new FacilityAvailabilityFilterRequest()))
                .extracting(FacilitySlot::startTime, FacilitySlot::endTime, FacilitySlot::taken)
                .containsExactly(
                        tuple(dayStart(), at(1, 0), true),
                        tuple(at(1, 0), at(23, 0), false),
                        tuple(at(23, 0), dayEnd(), true)
                );
    }

    /* Reserving is blocked by a pending reservation too, so availability has to show it as taken. */
    @Test
    void shouldCountPendingReservationsAsTaken() {
        reservationPersistanceFactory.getNewReservation()
                .withRandomValues()
                .status(ReservationStatus.PENDING)
                .facilityId(facilityId)
                .userId(neighbourId)
                .startTime(at(10, 0))
                .endTime(at(12, 0))
                .buildAndSave();

        var request = FacilityAvailabilityFilterRequest.builder().availableOnly(true).build();

        assertThat(availability(request))
                .extracting(FacilitySlot::startTime, FacilitySlot::endTime)
                .containsExactly(
                        tuple(dayStart(), at(10, 0)),
                        tuple(at(12, 0), dayEnd())
                );
    }

    @Test
    void shouldRejectMoreThanOneNarrowingBecauseTheyExcludeEachOther() {
        var availableAndTaken = FacilityAvailabilityFilterRequest.builder().availableOnly(true).takenOnly(true).build();
        var availableAndMine = FacilityAvailabilityFilterRequest.builder().availableOnly(true).myBookingsOnly(true).build();
        var takenAndMine = FacilityAvailabilityFilterRequest.builder().takenOnly(true).myBookingsOnly(true).build();
        var allThree = FacilityAvailabilityFilterRequest.builder().availableOnly(true).takenOnly(true).myBookingsOnly(true).build();

        assertThatThrownBy(() -> availability(availableAndTaken)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> availability(availableAndMine)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> availability(takenAndMine)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> availability(allThree)).isInstanceOf(BadRequestException.class);
    }

    /* Explicit false is not a narrowing - only a flag set to true picks a view. */
    @Test
    void shouldAcceptOneNarrowingNextToExplicitlyDisabledOnes() {
        reservation(facilityId, viewerId, at(14, 0), at(15, 0));

        var request = FacilityAvailabilityFilterRequest.builder()
                .myBookingsOnly(true)
                .availableOnly(false)
                .takenOnly(false)
                .build();

        assertThat(availability(request))
                .extracting(FacilitySlot::startTime, FacilitySlot::endTime, FacilitySlot::taken)
                .containsExactly(tuple(at(14, 0), at(15, 0), true));
    }

    private List<FacilitySlot> availability(FacilityAvailabilityFilterRequest request) {
        request.setDay(DAY);
        var filter = facilityFilteringService.prepareFilter(facilityId, viewerId, request);
        return reservationService.getFacilityAvailability(facilityId, request.view(), filter);
    }

    private void reservation(UUID facility, UUID user, LocalDateTime startTime, LocalDateTime endTime) {
        reservationPersistanceFactory.getNewReservation()
                .withRandomValues()
                .facilityId(facility)
                .userId(user)
                .startTime(startTime)
                .endTime(endTime)
                .buildAndSave();
    }

    private static LocalDateTime at(int hour, int minute) {
        return DAY.atTime(hour, minute);
    }

    private static LocalDateTime dayStart() {
        return DAY.atStartOfDay();
    }

    private static LocalDateTime dayEnd() {
        return DAY.plusDays(1).atStartOfDay();
    }
}
