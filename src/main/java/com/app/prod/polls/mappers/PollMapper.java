package com.app.prod.polls.mappers;

import com.app.prod.polls.dto.PollOptionResponse;
import com.app.prod.polls.dto.PollRequest;
import com.app.prod.polls.dto.PollResponse;
import org.jooq.sources.tables.records.PollOptionRecord;
import org.jooq.sources.tables.records.PollRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class PollMapper {

    public static PollRecord fromRequestToRecord(PollRequest request, UUID userId, LocalDateTime now, UUID pollId, UUID areaId){
        return new PollRecord(
                pollId,
                request.title(),
                request.description(),
                areaId,
                request.buildingId(),
                userId,
                request.startTime(),
                request.endTime(),
                request.anonymous(),
                false,
                now
        );
    }

    public static List<PollOptionResponse> fromListOfRecordsToListOfResponse(List<PollOptionRecord> records){
        return records.stream().map(PollMapper::fromRecordToResponse).toList();
    }

    public static PollOptionResponse fromRecordToResponse(PollOptionRecord record){
        return new PollOptionResponse(
                record.getId(),
                record.getOptionText()
        );
    }

}
