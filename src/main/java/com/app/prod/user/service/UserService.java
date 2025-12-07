package com.app.prod.user.service;

import com.app.prod.authorization.service.ActivationService;
import com.app.prod.config.security.jwt.JwtService;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.user.dto.*;
import com.app.prod.user.enums.UserRole;
import com.app.prod.user.enums.UserStatus;
import com.app.prod.user.mappers.UserMapper;
import com.app.prod.user.repository.UserRepository;
import com.app.prod.utils.PasswordGenerator;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.AppUser;
import org.jooq.sources.tables.records.AppUserRecord;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final Clock clock;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder encoder;
    private final UserRoleService userRoleService;
    private final ActivationService activationService;
    private final Validate validate;

    public List<AppUserRecord> getUsers(){
         return userRepository.findAll();
    }

    public String register(UserRegisterRequest request) {
        validate.thatUsernameIsFree(request.username());
        //TODO: NO CHECK IF EMAIL IS THE SAME WHAT LEEDS TO DB EXCEPTION
        LocalDateTime now = LocalDateTime.now(clock);
        UUID id = UUID.randomUUID();
        UUID selectedRoleId = userRoleService.getUserRoleId(request.role());
        userRepository.insertOne(UserMapper.fromRequestToRecord(request, id, now, selectedRoleId, encoder));

        log.info("Created user: {}", id);
        return String.format("User with id: %s has been created.", id);
    }

    public String login(UserLoginRequest request){
        return generateToken(request.username(), request.password());
    }

    @Transactional
    public String activate(UserActivateRequest request){
        activationService.activate(request);
        return generateToken(request.username(), request.password());
    }

    public String generateToken(String username, String password){
        var auth = new UsernamePasswordAuthenticationToken(username, password);
        Authentication result = authenticationManager.authenticate(auth);
        UserDetails ud = (UserDetails) result.getPrincipal();
        return jwtService.generateToken((org.springframework.security.core.userdetails.User) ud);
    }

    public AppUserRecord findByUsername(String username){
        return userRepository.findByUsername(username).orElseThrow(
                () -> new UsernameNotFoundException(String.format("User with username: %s does not exist.", username))
        );
    }

    public AppUserRecord findById(UUID userId){
        return userRepository.findById(userId).orElseThrow(
                () -> new EntityNotPresentException(
                        String.format("User with id: %s does not exist.", userId),
                        AppUser.class.getSimpleName()
                )
        );
    }

    public void addUsersBuilding(UUID userId, UUID buildingId){
        userRepository.addBuilding(userId, buildingId);
    }

    public BulkCreationResponse bulkCreation(BulkCreationRequest request) {
        List<PersonToBeCreated> failedCreations = new ArrayList<>();

        for(PersonToBeCreated personToBeCreated : request.personToBeCreateds()){
            int status = userRepository.insertOne(createNewNonActiveUser(personToBeCreated));
            if(status == 0){
                log.warn("User {} {} was not created.", personToBeCreated.firstName(), personToBeCreated.lastName());
                failedCreations.add(personToBeCreated);
            }
            else{
                log.info("User {} {} was has been created.", personToBeCreated.firstName(), personToBeCreated.lastName());
            }
        }

        return BulkCreationResponse.builder()
                .success(failedCreations.isEmpty())
                .failedCreations(failedCreations)
                .build();
    }

    private AppUserRecord createNewNonActiveUser(PersonToBeCreated personToBeCreated){
        String temporaryUsername = personToBeCreated.firstName().toLowerCase().charAt(0) + "." + personToBeCreated.lastName();
        var standardRole = userRoleService.getUserRoleId(UserRole.RESIDENT);
        var id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now(clock);

        return new AppUserRecord(
                id,
                personToBeCreated.firstName(),
                personToBeCreated.lastName(),
                null,
                temporaryUsername,
                PasswordGenerator.generatePassword(),
                standardRole,
                UserStatus.CREATED.name(),
                now,
                personToBeCreated.buildingId()
        );
    }

    public List<ResidentToAdd> getUsersWithoutBuilding() {
        return userRepository.getUsersWithoutBuilding();
    }
}
