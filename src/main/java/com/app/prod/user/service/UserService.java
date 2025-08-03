package com.app.prod.user.service;

import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.user.dto.UserRequest;
import com.app.prod.user.mappers.UserMapper;
import com.app.prod.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.UsersRecord;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


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
        userRepository.insert(UserMapper.fromRequestToRecord(request, id, now));

        return String.format("User with id: %s has been created.", id);
    }
}
