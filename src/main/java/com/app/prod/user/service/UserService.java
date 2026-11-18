package com.app.prod.user.service;

import com.app.prod.authorization.service.ActivationService;
import com.app.prod.config.security.jwt.JwtService;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.internal.dtos.UserDto;
import com.app.prod.job.async.AsyncJobRunner;
import com.app.prod.job.jobs.SyncUserJob;
import com.app.prod.user.dto.*;
import com.app.prod.user.enums.UserRole;
import com.app.prod.user.enums.UserStatus;
import com.app.prod.user.mappers.UserMapper;
import com.app.prod.user.repository.UserRepository;
import com.app.prod.utils.Emails;
import com.app.prod.utils.filters.BuildingUserFilter;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.AppUserRecord;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final Clock clock;
    private final BCryptPasswordEncoder encoder;
    private final UserRoleService userRoleService;
    private final ActivationService activationService;
    private final Validate validate;
    private final AsyncJobRunner asyncJobRunner;
    private final JwtService jwtTokenService;

    public List<AppUserRecord> getUsers(){
         return userRepository.findAll();
    }

    public void register(UserRegisterRequest request) {
        String email = Emails.normalize(request.email());
        validate.thatUsernameIsFree(request.username());
        validate.thatEmailIsFree(email);
        LocalDateTime now = LocalDateTime.now(clock);
        UUID id = UUID.randomUUID();
        UUID selectedRoleId = userRoleService.getUserRoleId(UserRole.RESIDENT);

        String encodedPassword = encoder.encode(request.password());
        userRepository.insertOne(UserMapper.fromRequestToRecord(request, email, id, now, selectedRoleId, encodedPassword));

        SyncUserJob job = new SyncUserJob(new UserDto(
                id,
                request.firstName(),
                request.lastName(),
                email,
                request.username(),
                encodedPassword,
                selectedRoleId,
                UserStatus.ACTIVE,
                now
        ));
        asyncJobRunner.execute(job);
        log.info("Created user: {}", id);
    }


    @Transactional
    public String activate(UserActivateRequest request){
        activationService.activate(request);
        return jwtTokenService.generateJwtAccessToken(request.username(), request.password());
    }

    public void addUsersBuilding(UUID userId, UUID buildingId){
        userRepository.addBuilding(userId, buildingId);
    }

    public List<BuildingUserResponse> getUsersInBuilding(BuildingUserFilter filter) {
        return userRepository.findUsersInBuilding(filter);
    }

    public List<ResidentToAdd> getUsersWithoutBuilding() {
        return userRepository.getUsersWithoutBuilding();
    }

    public List<UUID> getAllUsersInAreaWithoutUser(UUID userId, UUID areaId){
        return userRepository.getAllUsersInArea(areaId).stream()
                .filter(u -> !u.equals(userId))
                .collect(Collectors.toList());
    }

    public UserMetaInfo getUserMetadata(AppUserRecord user) {
        return userRepository.getUserMetaData(user.getId());
    }

    public AppUserRecord findByUsername(String username){
        return userRepository.findByUsername(username).orElseThrow(
                () -> new UsernameNotFoundException(String.format("User with username: %s does not exist.", username))
        );
    }

    public AppUserRecord findById(UUID userId){
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.USER_NOT_FOUND)));
    }
}
