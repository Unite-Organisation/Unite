package com.app.prod.polls.mappers;

import com.app.prod.polls.dto.PollRequest;
import org.jooq.sources.tables.records.PollsRecord;

import java.time.LocalDateTime;
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

}
