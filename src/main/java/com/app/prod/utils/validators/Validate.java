package com.app.prod.utils.validators;

import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.DataAlreadyExistsException;
import com.app.prod.post.repository.PostRepository;
import com.app.prod.area.repository.AreaRepository;
import com.app.prod.building.repository.BuildingRepository;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.exceptions.exceptions.UnauthorizedDataAccessException;
import com.app.prod.facilities.repository.FacilityRepository;
import com.app.prod.issues.repository.IssueRepository;
import com.app.prod.issues.repository.NotificationRepository;
import com.app.prod.offering.repository.OfferingRepository;
import com.app.prod.polls.repository.PollRepository;
import com.app.prod.requests.repository.RequestRepository;
import com.app.prod.user.enums.UserRole;
import com.app.prod.user.repository.UserRepository;
import com.app.prod.user.repository.UserRoleRepository;
import com.app.prod.user.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.*;
import org.jooq.sources.tables.records.IssueRecord;
import org.jooq.sources.tables.records.OfferingRecord;
import org.jooq.sources.tables.records.UserRoleRecord;
import org.jooq.sources.tables.records.AppUserRecord;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class Validate {

    private final UserRepository userRepository;
    private final AreaRepository areaRepository;
    private final BuildingRepository buildingRepository;
    private final PollRepository pollRepository;
    private final FacilityRepository facilityRepository;
    private final PostRepository postRepository;
    private final OfferingRepository offeringRepository;
    private final UserRoleRepository userRoleRepository;
    private final IssueRepository issueRepository;
    private final NotificationRepository notificationRepository;
    private final RequestRepository requestRepository;
    private final UserRoleService userRoleService;

    public void user(UUID id){
        if(!userRepository.exists(id)){
            throw new EntityNotPresentException(AppError.of(Code.USER_NOT_FOUND, String.format("User with id: %s not found. Entity: %s", id, AppUser.class.getSimpleName())));
        }
    }

    public void post(UUID id) {
        if (!postRepository.exists(id)) {
            throw new EntityNotPresentException(AppError.of(
                    Code.POST_NOT_FOUND,
                    String.format("Post with id: %s doesn't exist. Entity: %s", id, Post.class.getSimpleName())
            ));
        }
    }

    public void area(UUID id) {
        if (!areaRepository.exists(id)) {
            throw new EntityNotPresentException(AppError.of(
                    Code.AREA_NOT_FOUND,
                    String.format("Area with id: %s doesn't exist. Entity: %s", id, Area.class.getSimpleName())
            ));
        }
    }

    public void facility(UUID id) {
        if (!facilityRepository.exists(id)) {
            throw new EntityNotPresentException(AppError.of(
                    Code.FACILITY_NOT_FOUND,
                    String.format("Facility with id: %s doesn't exist. Entity: %s", id, Facility.class.getSimpleName())
            ));
        }
    }

    public void notification(UUID id) {
        if (!notificationRepository.exists(id)) {
            throw new EntityNotPresentException(AppError.of(
                    Code.NOTIFICATION_NOT_FOUND,
                    String.format("Notification with id: %s doesn't exist. Entity: %s", id, Notification.class.getSimpleName())
            ));
        }
    }

    public void building(UUID id) {
        if (!buildingRepository.exists(id)) {
            throw new EntityNotPresentException(AppError.of(
                    Code.BUILDING_NOT_FOUND,
                    String.format("Building with id: %s doesn't exist. Entity: %s", id, Building.class.getSimpleName())
            ));
        }
    }

    public void thatThisRequestBelongsToUser(AppUserRecord user, UUID requestId) {
        var request = requestRepository.findById(requestId).orElseThrow(
                () -> new EntityNotPresentException(AppError.of(
                        Code.REQUEST_NOT_FOUND,
                        String.format("Request with id: %s does not exist. Entity: %s", requestId, Request.class.getSimpleName())
                ))
        );

        if (!request.getUserInNeed().equals(user.getId())) {
            throw new UnauthorizedDataAccessException(AppError.of(
                    Code.ACCESS_DENIED,
                    String.format("User %s does not have access to this request", user.getId())
            ));
        }
    }

    public void thatUserHasBuilding(AppUserRecord user) {
        if (user.getBuildingId() == null) {
            throw new UnauthorizedDataAccessException(AppError.of(
                    Code.USER_WITHOUT_BUILDING,
                    String.format("User %s does not have access to any building", user.getId())
            ));
        }
    }

    public void thatUserHasBuildingAssigned(AppUserRecord user) {
        if (user.getBuildingId() == null) {
            throw new BadRequestException(AppError.of(
                    Code.USER_WITHOUT_BUILDING,
                    "You don't have access to any building"
            ));
        }
    }

    public void thatUsernameIsFree(String username) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new DataAlreadyExistsException(AppError.of(Code.USERNAME_TAKEN));
        }
    }

    public void thatEmailIsFree(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new DataAlreadyExistsException(AppError.of(Code.EMAIL_TAKEN));
        }
    }

    public void thatUserIsNotInAnyBuildingYet(UUID userId) {
        var user = userRepository.findById(userId).orElseThrow(
                () -> new EntityNotPresentException(AppError.of(
                        Code.USER_NOT_FOUND,
                        String.format("User with id: %s does not exist. Entity: %s", userId, AppUser.class.getSimpleName())
                ))
        );
        if (user.getBuildingId() != null) {
            throw new BadRequestException(AppError.of(
                    Code.USER_WITH_BUILDING,
                    "User has already been set to building: " + user.getBuildingId()
            ));
        }
    }

    public void thatUserCanVote(AppUserRecord user, UUID poll) {
        UserRole userRole = userRoleService.getUserRoleFromId(user.getUserRole());

        if (!UserRole.RESIDENT.equals(userRole)) {
            throw new BadRequestException(AppError.of(
                    Code.MANAGER_CANNOT_VOTE,
                    "Manager can't vote."
            ));
        }
        //TODO: add logic so users from another area's or buildings can not vote
    }

    public void poll(UUID pollId) {
        var poll = pollRepository.findById(pollId).orElseThrow(
                () -> new EntityNotPresentException(AppError.of(
                        Code.POLL_NOT_FOUND,
                        String.format("Poll with id %s not found. Entity: %s", pollId, Poll.class.getSimpleName())
                ))
        );

        if (!poll.getFinished()) {
            throw new BadRequestException(AppError.of(
                    Code.POLL_NOT_FINISHED,
                    String.format("Poll %s is not finished yet. Voting ends at %s", poll.getTitle(), poll.getEndTime())
            ));
        }
    }

    public void pollIssueCreation(UUID pollId) {
        pollRepository.findById(pollId).orElseThrow(() -> new EntityNotPresentException(AppError.of(
                Code.POLL_NOT_FOUND,
                String.format("Poll with id %s not found. Entity: %s", pollId, Poll.class.getSimpleName())
        )));
    }

    public void thatUserBelongsToBuilding(AppUserRecord user, UUID buildingId) {
        var userBuilding = user.getBuildingId();
        //TODO: manager should be able to access all his buildings

        if (userBuilding == null || !userBuilding.equals(buildingId)) {
            throw new UnauthorizedDataAccessException(AppError.of(
                    Code.ACCESS_DENIED,
                    String.format("User %s does not have access to building %s", user.getId(), buildingId)
            ));
        }
    }

    public void thatUserBelongsToArea(UUID areaId, AppUserRecord user) {
        thatUserHasBuilding(user);
        var building = buildingRepository.findById(user.getBuildingId()).orElseThrow(
                () -> new EntityNotPresentException(AppError.of(
                        Code.BUILDING_NOT_FOUND,
                        String.format("Building with id: %s doesn't exist.", user.getBuildingId())
                ))
        );

        if (!building.getAreaId().equals(areaId)) {
            throw new UnauthorizedDataAccessException(AppError.of(
                    Code.ACCESS_DENIED,
                    String.format("User %s does not have access to area %s", user.getId(), areaId)
            ));
        }
    }

    public void thatUserCanUseFacility(AppUserRecord user, UUID facilityId) {
        thatUserHasBuilding(user);
        var facility = facilityRepository.findById(facilityId).orElseThrow(
                () -> new EntityNotPresentException(AppError.of(
                        Code.FACILITY_NOT_FOUND,
                        String.format("Facility with id: %s doesn't exist.", facilityId)
                ))
        );

        if (!facility.getBuildingId().equals(user.getBuildingId())) {
            throw new UnauthorizedDataAccessException(AppError.of(
                    Code.ACCESS_DENIED,
                    String.format("User %s does not have access to facility %s", user.getId(), facilityId)
            ));
        }
    }

    public OfferingRecord andGetOffer(UUID offeringId, AppUserRecord user) {
        var offering = offeringRepository.findById(offeringId).orElseThrow(() -> new EntityNotPresentException(AppError.of(
                Code.OFFERING_NOT_FOUND,
                String.format("Offering with id: %s doesn't exist. Entity: %s", offeringId, Offering.class.getSimpleName())
        )));

        UserRoleRecord userRole = userRoleRepository.findById(user.getUserRole()).orElseThrow(
                () -> new EntityNotPresentException(AppError.of(
                        Code.USER_ROLE_NOT_FOUND,
                        String.format("User role with id: %s doesn't exist.", user.getUserRole())
                ))
        );

        if (UserRole.MANAGER.name().equals(userRole.getUserRole())) {
            return offering;
        }

        if (!offering.getUserProvider().equals(user.getId())) {
            throw new UnauthorizedDataAccessException(AppError.of(
                    Code.ACCESS_DENIED,
                    String.format("User %s does not have access to offering %s", user.getId(), offering.getId())
            ));
        }

        return offering;
    }

    public IssueRecord andGetIssue(UUID issueId) {
        return issueRepository.findById(issueId).orElseThrow(() -> new EntityNotPresentException(AppError.of(
                Code.ISSUE_NOT_FOUND,
                String.format("Issue with id: %s doesn't exist. Entity: %s", issueId, Issue.class.getSimpleName())
        )));
    }
}
