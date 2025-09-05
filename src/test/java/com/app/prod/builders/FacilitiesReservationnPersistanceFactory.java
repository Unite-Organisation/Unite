package com.app.prod.builders;

import com.app.prod.facilities.enums.ReservationStatus;
import com.app.prod.facilities.repository.FacilityReservationsRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.FacilitiesReservationsRecord;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FacilitiesReservationnPersistanceFactory {

    private final Clock clock;
    private final FacilityReservationsRepository facilityReservationsRepository;

    public Builder getNewReservation(){ return new Builder(); }

    public class Builder {

        private final FacilitiesReservationsRecord instance;

        public Builder() {
            instance = new FacilitiesReservationsRecord();
        }

        public Builder facilityId(UUID id) {
            instance.setFacilityId(id);
            return this;
        }

        public Builder userId(UUID id){
            instance.setUserId(id);
            return this;
        }

        public Builder startTime(LocalDateTime start){
            instance.setStartTime(start);
            return this;
        }

        public Builder endTime(LocalDateTime end){
            instance.setEndTime(end);
            return this;
        }

        public Builder withRandomValues(){
            instance.setId(UUID.randomUUID());
            instance.setStatus(ReservationStatus.RESERVED.name());
            instance.setPurpose("purpose");
            instance.setCreatedAt(LocalDateTime.now(clock));
            return this;
        }

        public FacilitiesReservationsRecord build() {
            return instance;
        }

        public FacilitiesReservationsRecord buildAndSave() {
            FacilitiesReservationsRecord record = build();
            facilityReservationsRepository.insertOne(record);
            return record;
        }
    }
}
