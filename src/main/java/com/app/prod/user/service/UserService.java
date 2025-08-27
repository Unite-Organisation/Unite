package com.app.prod.user.service;

import com.app.prod.config.security.jwt.JwtService;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.user.dto.UserLoginRequest;
import com.app.prod.user.dto.UserRegisterRequest;
import com.app.prod.user.enums.UserRole;
import com.app.prod.user.mappers.UserMapper;
import com.app.prod.user.repository.UserRepository;
import com.app.prod.user.repository.UserRoleRepository;
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

import java.time.Clock;
import java.time.LocalDateTime;
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

    public List<UsersRecord> getUsers(){
         return userRepository.findAll();
    }

    public String register(UserRegisterRequest request) {

        if(userRepository.findByUsername(request.username()).isPresent()){
            throw new BadRequestException("This username is already taken.");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        UUID id = UUID.randomUUID();
        UUID selectedRoleId = userRoleService.getUserRoleId(request.role());
        userRepository.insertOne(UserMapper.fromRequestToRecord(request, id, now, selectedRoleId, encoder));

        log.info("Created user: {}", id);
        return String.format("User with id: %s has been created.", id);
    }

    public String login(UserLoginRequest request){
        var auth = new UsernamePasswordAuthenticationToken(request.username(), request.password());
        Authentication result = authenticationManager.authenticate(auth);
        UserDetails ud = (UserDetails) result.getPrincipal();
        return jwtService.generateToken((org.springframework.security.core.userdetails.User) ud);
    }

    public UsersRecord findByUsername(String username){
        return userRepository.findByUsername(username).orElseThrow(
                () -> new UsernameNotFoundException(String.format("User with username: %s does not exist.", username))
        );
    }
}
