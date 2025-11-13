package com.app.prod.facilities;

import com.app.prod.config.security.TokenSecurityManager;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.facilities.dto.ReservationRequest;
import com.app.prod.facilities.dto.ReserveResponse;
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
    @Mock
    private TokenSecurityManager tokenSecurityManager;

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

        ReserveResponse response = reservationService.reserve(request, userId);
        assertThat(response.success()).isTrue();
        assertThat(response.reservations()).isNotNull();
        assertThat(response.reservations()).hasSize(1);
        assertThat(response.reservations().getFirst().status()).isEqualTo(ReservationStatus.RESERVED.name());
        assertThat(response.reservations().getFirst().userId()).isEqualTo(userId);
        assertThat(response.reservations().getFirst().startTime()).isEqualTo(start);
        assertThat(response.reservations().getFirst().endTime()).isEqualTo(end);
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
        ReserveResponse response = reservationService.reserve(request, userId);
        assertThat(response.success()).isFalse();
        assertThat(response.reservations()).isNotNull();
        assertThat(response.reservations()).isNotEmpty();
        assertThat(response.reservations().getFirst().facilityId()).isEqualTo(facilityId);
        assertThat(response.reservations().getFirst().userId()).isEqualTo(userThatReservedBeforeMe);
        assertThat(response.reservations().getFirst().startTime()).isEqualTo(existingStart);
        assertThat(response.reservations().getFirst().endTime()).isEqualTo(existingEnd);
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
