package com.app.prod.user.service;

import com.app.prod.authorization.service.ActivationService;
import com.app.prod.config.security.jwt.JwtService;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.user.dto.*;
import com.app.prod.user.enums.UserRole;
import com.app.prod.user.enums.UserStatus;
import com.app.prod.user.mappers.UserMapper;
import com.app.prod.user.repository.UserRepository;
import com.app.prod.user.repository.UserRoleRepository;
import com.app.prod.utils.PasswordGenerator;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.UsersRecord;
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

    public List<UsersRecord> getUsers(){
         return userRepository.findAll();
    }

    public String register(UserRegisterRequest request) {
        validate.thatUsernameIsFree(request.username());

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

    public UsersRecord findByUsername(String username){
        return userRepository.findByUsername(username).orElseThrow(
                () -> new UsernameNotFoundException(String.format("User with username: %s does not exist.", username))
        );
    }

    public BulkCreationResponse bulkCreation(BulkCreationRequest request) {
        List<Person> failedCreations = new ArrayList<>();

        for(Person person : request.persons()){
            int status = userRepository.insertOne(createNewNonActiveUser(person));
            if(status == 0){
                log.warn("User {} {} was not created.", person.firstName(), person.lastName());
                failedCreations.add(person);
            }
            else{
                log.info("User {} {} was has been created.", person.firstName(), person.lastName());
            }
        }

        return BulkCreationResponse.builder()
                .success(failedCreations.isEmpty())
                .failedCreations(failedCreations)
                .build();
    }

    private UsersRecord createNewNonActiveUser(Person person){
        String temporaryUsername = person.firstName().toLowerCase().charAt(0) + "." + person.lastName();
        UUID standardRole = userRoleService.getUserRoleId(UserRole.STANDARD);
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now(clock);
        return new UsersRecord(
                id,
                person.firstName(),
                person.lastName(),
                null,
                PasswordGenerator.generatePassword(),
                standardRole,
                now,
                temporaryUsername,
                UserStatus.CREATED.name()
        );
    }
}
