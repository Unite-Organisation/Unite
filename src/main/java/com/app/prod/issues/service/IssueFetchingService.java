package com.app.prod.issues.service;

import com.app.prod.issues.dto.IssueResponse;
import com.app.prod.issues.repository.IssueRepository;
import com.app.prod.issues.repository.strategy.AreaJoiningStrategy;
import com.app.prod.issues.repository.strategy.BuildingJoiningStrategy;
import com.app.prod.issues.repository.strategy.FacilityJoiningStrategy;
import com.app.prod.issues.repository.strategy.PollJoiningStrategy;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.UsersRecord;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class IssueFetchingService {

    private final IssueRepository issueRepository;
    private final Validate validate;

    private final AreaJoiningStrategy areaJoiningStrategy;
    private final BuildingJoiningStrategy buildingJoiningStrategy;
    private final FacilityJoiningStrategy facilityJoiningStrategy;
    private final PollJoiningStrategy pollJoiningStrategy;

    public List<IssueResponse> getBuildingIssues(UUID buildingId, UsersRecord user) {
        validate.thatUserBelongsToBuilding(user, buildingId);
        return issueRepository.getEntityIssues(buildingId, buildingJoiningStrategy);
    }

    public List<IssueResponse> getAreaIssues(UUID areaId, UsersRecord user) {
        validate.thatUserBelongsToArea(areaId, user);
        return issueRepository.getEntityIssues(areaId, areaJoiningStrategy);
    }

    public List<IssueResponse> getFacilityIssues(UUID facilityId, UsersRecord user) {
        validate.thatUserCanUseFacility(user, facilityId);
        return issueRepository.getEntityIssues(facilityId, facilityJoiningStrategy);
    }

    public List<IssueResponse> getPollIssues(UUID pollId, UsersRecord user) {
        validate.thatUserCanVote(user.getId(), pollId);
        return issueRepository.getEntityIssues(pollId, pollJoiningStrategy);
    }
}
