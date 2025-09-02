package com.app.prod.facilities.service;

import com.app.prod.config.security.TokenSecurityManager;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.facilities.dto.FacilityReservation;
import com.app.prod.facilities.dto.ReservationRequest;
import com.app.prod.facilities.dto.ReserveResponse;
import com.app.prod.facilities.enums.ReservationStatus;
import com.app.prod.facilities.mappers.ReservationMapper;
import com.app.prod.facilities.repository.FacilityRepository;
import com.app.prod.facilities.repository.FacilityReservationsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.meta.derby.sys.Sys;
import org.jooq.sources.tables.records.FacilitiesReservationsRecord;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final FacilityReservationsRepository facilityReservationsRepository;
    private final FacilityRepository facilityRepository;
    private final TokenSecurityManager tokenSecurityManager;
    private final Clock clock;

    public List<FacilityReservation> getFacilityAvailability(UUID facilityId) {
        return facilityReservationsRepository.getAvailability(facilityId);
    }

    public ReserveResponse reserve(ReservationRequest request) {
        UUID facilityId = request.facilityId();
        LocalDateTime startTime = request.startTime();
        LocalDateTime endTime = request.endTime();

        System.out.println("HELLO");

        if(startTime.isAfter(endTime)){
            log.debug("Start: {} is after endtime: {}", startTime, endTime);
            System.out.println("HERE");
            throw new BadRequestException(String.format("%s is after %s", startTime, endTime));
        }

        List<FacilitiesReservationsRecord> overlappingReservations =
                facilityReservationsRepository.getOverlappingReservationsForFacility(facilityId, startTime, endTime);

        if(!overlappingReservations.isEmpty()){
            overlappingReservations.forEach(reservation -> {
                log.warn("Selected date for facility: {} is booked by {}", reservation.getFacilityId(), reservation.getUserId());
            });
            return new ReserveResponse(
                    false,
                    ReservationMapper.fromRecordToResponse(overlappingReservations)
            );
        }

        ReservationStatus status = determineReservationStatusFromFacility(facilityId);
        var recordToBeInserted = new FacilitiesReservationsRecord(
                UUID.randomUUID(),
                facilityId,
                tokenSecurityManager.getCurrentUser().getId(),
                startTime,
                endTime,
                status.name(),
                request.purpose(),
                LocalDateTime.now(clock)
        );

        facilityReservationsRepository.insertOne(recordToBeInserted);
        return new ReserveResponse(
                true,
                ReservationMapper.fromRecordToResponse(List.of(recordToBeInserted))
        );
    }

    private ReservationStatus determineReservationStatusFromFacility(UUID facilityId){
        var facility = facilityRepository.findById(facilityId);
        return facility.get().getRequiresApproval() ?
                ReservationStatus.PENDING : ReservationStatus.RESERVED;
    }

    private boolean intervalsOverlap(
            LocalDateTime desirableStart,
            LocalDateTime desirableEnd,
            LocalDateTime xStart,
            LocalDateTime xEnd
            ){
        return !((desirableEnd.isBefore(xStart) && desirableStart.isBefore(xStart)) ||
                (desirableStart.isAfter(xEnd) && desirableEnd.isAfter(xEnd)));
    }
}
