package com.app.prod.facilities.mappers;

import com.app.prod.facilities.dto.ReserveResponse;
import org.jooq.sources.tables.records.FacilityReservationRecord;

import java.util.List;

public class ReservationMapper {

    public static List<ReserveResponse.ReservationResponse> fromRecordToResponse(List<FacilityReservationRecord> records){
        return records.stream()
                .map(record -> new ReserveResponse.ReservationResponse(
                        record.getId(),
                        record.getFacilityId(),
                        record.getUserId(),
                        record.getStartTime(),
                        record.getEndTime(),
                        record.getStatus(),
                        record.getPurpose(),
                        record.getCreatedAt()
                )).toList();
    }

}
