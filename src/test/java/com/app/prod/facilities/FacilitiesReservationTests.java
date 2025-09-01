package com.app.prod.facilities;

import com.app.prod.config.security.TokenSecurityManager;
import com.app.prod.facilities.dto.ReservationRequest;
import com.app.prod.facilities.dto.ReserveResponse;
import com.app.prod.facilities.enums.ReservationStatus;
import com.app.prod.facilities.repository.FacilityRepository;
import com.app.prod.facilities.repository.FacilityReservationsRepository;
import com.app.prod.facilities.service.ReservationService;
import org.jooq.sources.tables.records.FacilitiesRecord;
import org.jooq.sources.tables.records.UsersRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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

    @Test
    void shouldReserveSuccessfully(){
        UUID facilityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.of(2025, 9, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2025, 9, 1, 12, 0);

        FacilitiesRecord facilitiesRecord = new FacilitiesRecord(
                facilityId,
                "Billard",
                UUID.randomUUID(),
                "ENTERTAINMENT",
                2,
                "basement",
                false
        );

        ReservationRequest request = new ReservationRequest(
                facilityId,
                start,
                end,
                null
        );

        when(facilityReservationsRepository.getOverlappingReservationsForFacility(facilityId, start, end))
                .thenReturn(List.of());
        when(facilityRepository.findById(facilityId))
                .thenReturn(Optional.of(facilitiesRecord));
        when(tokenSecurityManager.getCurrentUser())
                .thenReturn(new UsersRecord(
                        userId,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ));
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(clock.instant()).thenReturn(Instant.parse("2025-09-01T00:00:00Z"));

        ReserveResponse response = reservationService.reserve(request);
        assertThat(response.success()).isTrue();
        assertThat(response.reservations()).isNotNull();
        assertThat(response.reservations()).hasSize(1);
        assertThat(response.reservations().getFirst().status()).isEqualTo(ReservationStatus.RESERVED.name());
        assertThat(response.reservations().getFirst().userId()).isEqualTo(userId);
        assertThat(response.reservations().getFirst().startTime()).isEqualTo(start);
        assertThat(response.reservations().getFirst().endTime()).isEqualTo(end);
    }
}
