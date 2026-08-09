package com.app.prod.facilities.mappers;

import com.app.prod.facilities.dto.ReservationResponse;
import com.app.prod.facilities.enums.ReservationStatus;
import org.jooq.sources.tables.records.FacilityReservationRecord;

public class ReservationMapper {

    public static ReservationResponse fromRecordToResponse(FacilityReservationRecord record){
        return new ReservationResponse(
                record.getId(),
                record.getFacilityId(),
                record.getUserId(),
                record.getStartTime(),
                record.getEndTime(),
                ReservationStatus.valueOf(record.getStatus()),
                record.getPurpose(),
                record.getCreatedAt()
        );
    }

}
