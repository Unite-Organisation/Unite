package com.app.prod.facilities.service;

import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.exceptions.exceptions.DataAlreadyExistsException;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.facilities.dto.FacilityReservation;
import com.app.prod.facilities.dto.FacilitySlot;
import com.app.prod.facilities.dto.ReservationRequest;
import com.app.prod.facilities.dto.ReservationResponse;
import com.app.prod.facilities.enums.ReservationStatus;
import com.app.prod.facilities.enums.SlotView;
import com.app.prod.facilities.mappers.ReservationMapper;
import com.app.prod.facilities.repository.FacilityRepository;
import com.app.prod.facilities.repository.FacilityReservationsRepository;
import com.app.prod.utils.filters.FacilityReservationFilter;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.FacilityReservationRecord;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final FacilityReservationsRepository facilityReservationsRepository;
    private final FacilityRepository facilityRepository;
    private final Validate validate;
    private final Clock clock;

    public static final long MAX_RESERVATION_HOURS = 12;

    public List<FacilitySlot> getFacilityAvailability(UUID facilityId, SlotView view, FacilityReservationFilter filter) {
        validate.facility(facilityId);
        List<FacilityReservation> reservations = facilityReservationsRepository.findReservations(filter);
        return AvailabilityCalculator.slotsOf(filter.getDay(), view, reservations);
    }

    public ReservationResponse reserve(ReservationRequest request, UUID userId) {
        UUID facilityId = request.facilityId();
        LocalDateTime startTime = request.startTime();
        LocalDateTime endTime = request.endTime();

        validateTimePeriods(startTime, endTime);
        ReservationStatus status = determineReservationStatusFromFacility(facilityId);
        validateThatPeriodIsFree(facilityId, startTime, endTime);

        var recordToBeInserted = new FacilityReservationRecord(
                UUID.randomUUID(),
                facilityId,
                userId,
                startTime,
                endTime,
                status.name(),
                request.purpose(),
                LocalDateTime.now(clock)
        );

        facilityReservationsRepository.insertOne(recordToBeInserted);
        return ReservationMapper.fromRecordToResponse(recordToBeInserted);
    }

    private void validateThatPeriodIsFree(UUID facilityId, LocalDateTime startTime, LocalDateTime endTime) {
        List<FacilityReservationRecord> overlappingReservations =
                facilityReservationsRepository.getOverlappingReservationsForFacility(facilityId, startTime, endTime);

        if (overlappingReservations.isEmpty()) {
            return;
        }

        overlappingReservations.forEach(reservation -> log.info(
                "Facility {} is booked by {} between {} and {}",
                reservation.getFacilityId(), reservation.getUserId(), reservation.getStartTime(), reservation.getEndTime()
        ));
        throw new DataAlreadyExistsException(AppError.of(
                Code.FACILITY_ALREADY_RESERVED,
                String.format("Facility %s is already reserved between %s and %s", facilityId, startTime, endTime)
        ));
    }

    private static void validateTimePeriods(LocalDateTime startTime, LocalDateTime endTime) {
        if(startTime.isAfter(endTime)){
            log.warn("Start: {} is after endtime: {}", startTime, endTime);
            throw new BadRequestException(AppError.of(Code.INVALID_TIME_PERIOD, String.format("%s is after %s", startTime, endTime)));
        }

        Duration duration = Duration.between(startTime, endTime);
        long hours = duration.toHours();
        if(hours > MAX_RESERVATION_HOURS){
            log.warn("Period is longer than {} hours", MAX_RESERVATION_HOURS);
            throw new BadRequestException(AppError.of(Code.INVALID_TIME_PERIOD,  String.format("Period is longer than %s hours", MAX_RESERVATION_HOURS)));
        }
    }

    private ReservationStatus determineReservationStatusFromFacility(UUID facilityId){
        var facility = facilityRepository.findById(facilityId).orElseThrow(
                () -> new EntityNotPresentException(AppError.of(
                        Code.FACILITY_NOT_FOUND,
                        String.format("Facility with id: %s doesn't exist.", facilityId)
                ))
        );

        return facility.getRequiresApproval() ?
                ReservationStatus.PENDING : ReservationStatus.RESERVED;
    }
}
