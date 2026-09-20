package com.app.prod.issues.service;

import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.issues.dto.StrategyOptions;
import com.app.prod.issues.strategy.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class IssueStrategyFactory {

    private final AreaIssue areaIssue;
    private final BuildingIssue buildingIssue;
    private final FacilityIssue facilityIssue;
    private final PollIssue pollIssue;

    public IssueNotifyingStrategy chooseStrategy(StrategyOptions options){
        IssueNotifyingStrategy strategy = null;

        if(options.areaId() != null) {
            strategy = areaIssue;
            log.info("AreaIssue strategy was chosen");
        }

        if(options.buildingId() != null) {
            strategy = buildingIssue;
            log.info("BuildingIssue strategy was chosen");
        }

        if(options.facilityId() != null) {
            strategy = facilityIssue;
            log.info("FacilityIssue strategy was chosen");
        }

        if(options.pollId() != null) {
            strategy = pollIssue;
            log.info("PollIssue strategy was chosen");
        }

        if(strategy == null){
            throw new BadRequestException(AppError.of(Code.ISSUE_STRATEGY_NOT_CHOSEN, "At least one issue option must be chosen"));
        }

        return strategy;
    }
}
