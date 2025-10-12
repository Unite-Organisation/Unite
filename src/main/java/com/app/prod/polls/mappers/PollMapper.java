package com.app.prod.polls.mappers;

import com.app.prod.polls.dto.PollOptionResponse;
import com.app.prod.polls.dto.PollRequest;
import com.app.prod.polls.dto.PollResponse;
import org.jooq.sources.tables.records.PollOptionsRecord;
import org.jooq.sources.tables.records.PollsRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class PollMapper {

    public static PollsRecord fromRequestToRecord(PollRequest request, UUID userId, LocalDateTime now, UUID pollId){
        return new PollsRecord(
                pollId,
                request.title(),
                request.description(),
                request.areaId(),
                request.buildingId(),
                userId,
                request.startTime(),
                request.endTime(),
                request.anonymous(),
                false,
                now
        );
    }

    public static List<PollOptionResponse> fromListOfRecordsToListOfResponse(List<PollOptionsRecord> records){
        return records.stream().map(PollMapper::fromRecordToResponse).toList();
    }

    public static PollOptionResponse fromRecordToResponse(PollOptionsRecord record){
        return new PollOptionResponse(
                record.getId(),
                record.getOptionText()
        );
    }

}
