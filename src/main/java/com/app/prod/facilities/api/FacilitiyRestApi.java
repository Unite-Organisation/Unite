package com.app.prod.facilities.api;

import com.app.prod.facilities.dto.FacilityReservation;
import com.app.prod.facilities.dto.ReservationRequest;
import com.app.prod.facilities.dto.ReserveResponse;
import com.app.prod.facilities.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/facility")
@RequiredArgsConstructor
public class FacilitiyRestApi {

    private final ReservationService reservationService;

    @GetMapping("/{facilityId}")
    public ResponseEntity<List<FacilityReservation>> getAvailability(@PathVariable UUID facilityId){
        var reservations = reservationService.getFacilityAvailability(facilityId);
        return ResponseEntity.ok(reservations);
    }

    @PostMapping("/reserve")
    public ResponseEntity<ReserveResponse> reserveFacility(ReservationRequest request){
        ReserveResponse response = reservationService.reserve(request);

        if(response.success()){
            return ResponseEntity.ok(response);
        }
        else{
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

}
