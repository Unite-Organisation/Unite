package com.app.prod.facilities.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record FacilityReservation(
      UUID reservationId,
      String userFirstName,
      String userLastName,
      LocalDateTime startTime,
      LocalDateTime endTime
){}

