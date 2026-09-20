package com.app.prod.facilities;

import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.exceptions.exceptions.DataAlreadyExistsException;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.facilities.dto.ReservationRequest;
import com.app.prod.facilities.dto.ReservationResponse;
import com.app.prod.facilities.enums.ReservationStatus;
import com.app.prod.facilities.repository.FacilityRepository;
import com.app.prod.facilities.repository.FacilityReservationsRepository;
import com.app.prod.facilities.service.ReservationService;
import org.jooq.sources.tables.records.FacilityRecord;
import org.jooq.sources.tables.records.FacilityReservationRecord;
import org.jooq.sources.tables.records.AppUserRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.app.prod.facilities.service.ReservationService.MAX_RESERVATION_HOURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class FacilitiesReservationTests {

    @Mock
    private FacilityReservationsRepository facilityReservationsRepository;
    @Mock
    private FacilityRepository facilityRepository;
    @Mock
    private Clock clock;

    @InjectMocks
    private ReservationService reservationService;

    private UUID facilityId;
    private UUID userId;

    @BeforeEach()
    void setup(){
        facilityId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    private void stubClock(){
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(clock.instant()).thenReturn(Instant.parse("2025-09-01T00:00:00Z"));
    }

    private ReservationRequest stubFacilities(LocalDateTime start, LocalDateTime end, boolean requiresApproval, int capacity, List<FacilityReservationRecord> overlappingFacilities){
        FacilityRecord facilitiesRecord = new FacilityRecord(
                facilityId,
                "Billard",
                UUID.randomUUID(),
                "ENTERTAINMENT",
                capacity,
                "basement",
                requiresApproval
        );
        when(facilityRepository.findById(facilityId))
                .thenReturn(Optional.of(facilitiesRecord));

        when(facilityReservationsRepository.getOverlappingReservationsForFacility(facilityId, start, end))
                .thenReturn(overlappingFacilities);

        return new ReservationRequest(
                facilityId,
                start,
                end,
                null
        );
    }

    @Test
    void shouldReserveSuccessfully(){
        LocalDateTime start = LocalDateTime.of(2025, 9, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2025, 9, 1, 12, 0);

        stubClock();
        ReservationRequest request = stubFacilities(start, end, false, 2, List.of());

        ReservationResponse response = reservationService.reserve(request, userId);
        assertThat(response.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(response.facilityId()).isEqualTo(facilityId);
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.startTime()).isEqualTo(start);
        assertThat(response.endTime()).isEqualTo(end);
    }

    @Test
    void shouldReserveWithPendingStatusWhenFacilityRequiresApproval(){
        LocalDateTime start = LocalDateTime.of(2025, 9, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2025, 9, 1, 12, 0);

        stubClock();
        ReservationRequest request = stubFacilities(start, end, true, 2, List.of());

        assertThat(reservationService.reserve(request, userId).status()).isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    void shouldFailBecauseFacilityDoesNotExist(){
        LocalDateTime start = LocalDateTime.of(2025, 9, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2025, 9, 1, 12, 0);

        when(facilityRepository.findById(facilityId)).thenReturn(Optional.empty());

        ReservationRequest request = new ReservationRequest(facilityId, start, end, null);

        assertThrows(EntityNotPresentException.class,
                () -> reservationService.reserve(request, userId)
        );
    }

    @Test
    void shouldFailBecauseStartIsAfterEnd(){
        LocalDateTime start = LocalDateTime.of(2025, 9, 1, 12, 0);
        LocalDateTime end = LocalDateTime.of(2025, 9, 1, 10, 0);

        ReservationRequest request = new ReservationRequest(
                facilityId,
                start,
                end,
                null
        );

        assertThrows(BadRequestException.class,
                () -> reservationService.reserve(request, userId)
        );
    }

    @Test
    void shouldFailBecauseOfReservationTimeLimit(){
        LocalDateTime start = LocalDateTime.of(2025, 9, 1, 10, 0);
        LocalDateTime end = start.plusHours(MAX_RESERVATION_HOURS + 1);

        ReservationRequest request = new ReservationRequest(
                facilityId,
                start,
                end,
                null
        );

        assertThrows(BadRequestException.class,
                () -> reservationService.reserve(request, userId)
        );
    }

    @ParameterizedTest
    @MethodSource("overlappingDatesProvider")
    void shouldFailBecauseOfOverlappingDates(LocalDateTime start, LocalDateTime end,
                                             LocalDateTime existingStart, LocalDateTime existingEnd) {

        stubClock();
        var userThatReservedBeforeMe = UUID.randomUUID();
        List<FacilityReservationRecord> overlapping = List.of(
                new FacilityReservationRecord(
                    UUID.randomUUID(),
                    facilityId,
                    userThatReservedBeforeMe,
                    existingStart,
                    existingEnd,
                    ReservationStatus.RESERVED.name(),
                    null,
                    LocalDateTime.now(clock).minusDays(1)
                )
        );

        ReservationRequest request = stubFacilities(start, end, false, 2, overlapping);

        assertThrows(DataAlreadyExistsException.class,
                () -> reservationService.reserve(request, userId)
        );
        verify(facilityReservationsRepository, never()).insertOne(any());
    }

    public static Stream<Arguments> overlappingDatesProvider(){
        return Stream.of(
                Arguments.of(
                        LocalDateTime.of(2025, 9, 1, 10, 0),
                        LocalDateTime.of(2025, 9, 1, 14, 0),
                        LocalDateTime.of(2025, 9, 1, 12, 0),
                        LocalDateTime.of(2025, 9, 1, 16, 0)
                ),
                Arguments.of(
                        LocalDateTime.of(2025, 9, 1, 11, 0),
                        LocalDateTime.of(2025, 9, 1, 12, 0),
                        LocalDateTime.of(2025, 9, 2, 11, 0),
                        LocalDateTime.of(2025, 9, 2, 12, 0)
                )
        );

    }
}
