package com.app.prod.utils.validators;

import com.app.prod.area.repository.AreaRepository;
import com.app.prod.building.repository.BuildingRepository;
import com.app.prod.conversation.repository.ConversationMemberRepository;
import com.app.prod.conversation.repository.ConversationRepository;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.user.repository.UserRepository;
import com.app.prod.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final UserService userService;
    private final BuildingRepository buildingRepository;

    public void user(UUID id){
        if(!userRepository.exists(id)){
            throw new EntityNotPresentException(String.format("User with id: %s doesn't exist.", id));
        }
    }

    public void conversation(UUID id){
        if(!conversationRepository.exists(id)){
            throw new EntityNotPresentException(String.format("Conversation with id: %s doesn't exist.", id));
        }
    }

    public void area(UUID id){
        if(!areaRepository.exists(id)){
            throw new EntityNotPresentException(String.format("Area with id: %s doesn't exist.", id));
        }
    }

    public void thatUserBelongsToConversation(UUID userId, UUID conversationId){
        if(!conversationMemberRepository.userBelongToConversation(userId, conversationId)){
            throw new BadRequestException(String.format("User: %s does not belong to conversation: %s", userId, conversationId));
        }
    }

    public void thatUsernameIsFree(String username){
        if(userRepository.findByUsername(username).isPresent()){
            throw new BadRequestException("This username is already taken.");
        }
    }

    public void thatUserIsNotInAnyBuildingYet(UUID userId){
        var user = userService.findById(userId);
        if(user.getBuildingId() != null){
            throw new BadRequestException("User has already been set to building: " + user.getBuildingId());
        }
    }

    public void building(UUID id){
        if(!buildingRepository.exists(id)){
            throw new EntityNotPresentException(String.format("Building with id: %s doesn't exist.", id));
        }
    }

}
