package com.app.prod.facilities.dto;

import com.app.prod.facilities.enums.ReservationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record FacilityReservation(
      UUID reservationId,
      String userFirstName,
      String userLastName,
      LocalDateTime startTime,
      LocalDateTime endTime,
      ReservationStatus status
){}

