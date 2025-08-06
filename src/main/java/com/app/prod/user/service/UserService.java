package com.app.prod.user.service;

import com.app.prod.user.dto.UserRequest;
import com.app.prod.user.mappers.UserMapper;
import com.app.prod.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.UsersRecord;
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
    private final Clock clock;

    public List<UsersRecord> getUsers(){
         return userRepository.findAll();
    }

    public String createUser(UserRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        UUID id = UUID.randomUUID();
        userRepository.insertOne(UserMapper.fromRequestToRecord(request, id, now));

        log.info("Created user: {}", id);
        return String.format("User with id: %s has been created.", id);
    }
}
