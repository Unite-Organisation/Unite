package com.app.prod.user.service;

import com.app.prod.user.dto.UserLoginRequest;
import com.app.prod.user.dto.UserRequest;
import com.app.prod.user.mappers.UserMapper;
import com.app.prod.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.UsersRecord;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
//    private final AuthenticationManager authenticationManager;
    private final Clock clock;

    public List<UsersRecord> getUsers(){
         return userRepository.findAll();
    }

    //TODO: check if user exists in db by now
    public String register(UserRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        UUID id = UUID.randomUUID();
        userRepository.insertOne(UserMapper.fromRequestToRecord(request, id, now));

        log.info("Created user: {}", id);
        return String.format("User with id: %s has been created.", id);
    }

//    public String verify(UserLoginRequest login){
//        Authentication authentication = authenticationManager.authenticate(
//                new UsernamePasswordAuthenticationToken(
//                        login.username(),
//                        login.password()
//                )
//        );
//
//        if(authentication.isAuthenticated()){
//            log.info("User: {} has logged in", login.username());
//            return "Successful login";
//        }
//
//        log.info("User: {} has not been authenticated", login.username());
//        return "Login failed";
//    }
}
