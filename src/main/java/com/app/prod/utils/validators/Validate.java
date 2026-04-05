package com.app.prod.utils.validators;

import com.app.prod.exceptions.exceptions.DataAlreadyExistsException;
import com.app.prod.post.repository.PostRepository;
import com.app.prod.area.repository.AreaRepository;
import com.app.prod.building.repository.BuildingRepository;
import com.app.prod.conversation.repository.ConversationMemberRepository;
import com.app.prod.conversation.repository.ConversationRepository;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.exceptions.exceptions.UnauthorizedDataAccessException;
import com.app.prod.facilities.repository.FacilityRepository;
import com.app.prod.issues.repository.IssueRepository;
import com.app.prod.issues.repository.NotificationRepository;
import com.app.prod.offering.repository.OfferingRepository;
import com.app.prod.polls.repository.PollRepository;
import com.app.prod.requests.repository.RequestDonorRepository;
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

    private final ConversationMemberRepository conversationMemberRepository;
    private final ConversationRepository conversationRepository;
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
            throw new EntityNotPresentException(
                    String.format("User with id: %s doesn't exist.", id),
                    AppUser.class.getSimpleName()
            );
        }
    }

    public void conversation(UUID id){
        if(!conversationRepository.exists(id)){
            throw new EntityNotPresentException(
                    String.format("Conversation with id: %s doesn't exist.", id),
                    Conversation.class.getSimpleName()
            );
        }
    }

    public void post(UUID id){
        if(!postRepository.exists(id)){
            throw new EntityNotPresentException(
                    String.format("Post with id: %s doesn't exist.", id),
                    Post.class.getSimpleName()
            );
        }
    }

    public void area(UUID id){
        if(!areaRepository.exists(id)){
            throw new EntityNotPresentException(
                    String.format("Area with id: %s doesn't exist.", id),
                    Area.class.getSimpleName()
            );
        }
    }

    public void facility(UUID id){
        if(!facilityRepository.exists(id)){
            throw new EntityNotPresentException(
                    String.format("Facility with id: %s doesn't exist.", id),
                    Facility.class.getSimpleName()
            );
        }
    }

    public void notification(UUID id){
        if(!notificationRepository.exists(id)){
            throw new EntityNotPresentException(
                    String.format("Notification with id: %s doesn't exist.", id),
                    Notification.class.getSimpleName()
            );
        }
    }

    public void thatThisRequestBelongsToUser(AppUserRecord user, UUID requestId){
        var request = requestRepository.findById(requestId).orElseThrow(
                () -> new EntityNotPresentException(
                        String.format("Request with id: %s does not exist.", requestId),
                        Request.class.getSimpleName()
                ));

        if(!request.getUserInNeed().equals(user.getId())){
            throw new UnauthorizedDataAccessException(String.format("User %s does not have access to this request", user.getId()));
        }
    }

    public void thatUserHasBuilding(AppUserRecord user){
        if(user.getBuildingId() == null){
            throw new UnauthorizedDataAccessException(String.format("User %s does not have access to any building", user.getId()));
        }
    }

    public void thatUserHasBuildingAssigned(AppUserRecord user){
        if(user.getBuildingId() == null){
            throw new BadRequestException("You don't have access to any building");
        }
    }

    public void thatUserBelongsToConversation(UUID userId, UUID conversationId){
        if(!conversationMemberRepository.userBelongToConversation(userId, conversationId)){
            throw new BadRequestException(String.format("User: %s does not belong to conversation: %s", userId, conversationId));
        }
    }

    public void thatUsernameIsFree(String username){
        if(userRepository.findByUsername(username).isPresent()){
            throw new DataAlreadyExistsException("This username is already taken.");
        }
    }

    public void thatEmailIsFree(String email){
        if(userRepository.findByEmail(email).isPresent()){
            throw new DataAlreadyExistsException("Account with this email already exists.");
        }
    }

    public void thatUserIsNotInAnyBuildingYet(UUID userId){
        var user = userRepository.findById(userId).orElseThrow(
                () -> new EntityNotPresentException(
                        String.format("User with id: %s does not exist.", userId),
                        AppUser.class.getSimpleName()
                )
        );
        if(user.getBuildingId() != null){
            throw new BadRequestException("User has already been set to building: " + user.getBuildingId());
        }
    }

    public void building(UUID id){
        if(!buildingRepository.exists(id)){
            throw new EntityNotPresentException(
                    String.format("Building with id: %s doesn't exist.", id),
                    Building.class.getSimpleName()
            );
        }
    }

    public void thatUserCanVote(AppUserRecord user, UUID poll) {
        UserRole userRole = userRoleService.getUserRoleFromId(user.getUserRole());

        if (!UserRole.RESIDENT.equals(userRole)) {
            throw new BadRequestException("Manager can't vote.");
        }

        //TODO: add logic so users from another area's or buildings can not vote
    }

    public void poll(UUID pollId) {
        pollRepository.findById(pollId).ifPresentOrElse(poll -> {
            if(!poll.getFinished()){
                throw new BadRequestException(String.format("Poll %s is not finished yet. Voting ends at %s",
                        poll.getTitle(),
                        poll.getEndTime()
                ));
            }
        },
        () -> new EntityNotPresentException(
                String.format("Poll with id %d not found", pollId),
                Poll.class.getSimpleName()
        )
        );
    }

    public void pollIssueCreation(UUID pollId) {
        pollRepository.findById(pollId).orElseThrow(() -> new EntityNotPresentException(
                String.format("Poll with id %d not found", pollId),
                Poll.class.getSimpleName()
        ));
    }

    public void thatUserBelongsToBuilding(AppUserRecord user, UUID buildingId){
        var userBuilding = user.getBuildingId();

        //TODO: manager should be able to access all his buildings

        if(userBuilding == null || !userBuilding.equals(buildingId)){
            throw new UnauthorizedDataAccessException(String.format("User %s does not have access to building %s", user.getId(), buildingId));
        }
    }

    //TODO: fix bug when user want to see issues in area but he cannot
    public void thatUserBelongsToArea(UUID areaId, AppUserRecord user) {
        thatUserHasBuilding(user);
        var building = buildingRepository.findById(user.getBuildingId()).orElseThrow();

        if(!building.getAreaId().equals(areaId)){
            throw new UnauthorizedDataAccessException(String.format("User %s does not have access to area %s", user.getId(), areaId));
        }
    }

    public void thatUserCanUseFacility(AppUserRecord user, UUID facilityId){
        thatUserHasBuilding(user);
        var facility = facilityRepository.findById(facilityId).orElseThrow();

        if(!facility.getBuildingId().equals(user.getBuildingId())){
            throw new UnauthorizedDataAccessException(String.format("User %s does not have access to facility %s", user.getId(), facilityId));
        }
    }

    public OfferingRecord andGetOffer(UUID offeringId, AppUserRecord user) {
        var offering = offeringRepository.findById(offeringId).orElseThrow(() -> new EntityNotPresentException(
                String.format("Offering with id: %s doesn't exist.", offeringId),
                Offering.class.getSimpleName()
        ));

        UserRoleRecord userRole = userRoleRepository.findById(user.getUserRole()).orElseThrow();
        if(UserRole.MANAGER.name().equals(userRole.getUserRole()))
            return offering;

        if(!offering.getUserProvider().equals(user.getId())){
            throw new UnauthorizedDataAccessException(String.format("User %s does not have access to offering %s", user.getId(), offering.getId()));
        }

        return offering;
    }

    public IssueRecord andGetIssue(UUID issueId){
        return issueRepository.findById(issueId).orElseThrow(() -> new EntityNotPresentException(
                String.format("Issue with id: %s doesn't exist.", issueId),
                Issue.class.getSimpleName()
        ));
    }
}
