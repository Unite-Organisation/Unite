package com.app.prod.user.api;

import com.app.prod.user.dto.UserRegisterRequest;
import com.app.prod.user.dto.UserResponse;
import com.app.prod.user.mappers.UserMapper;
import com.app.prod.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.UsersRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("user")
@RequiredArgsConstructor
public class UserRestApi {

    private final UserService userService;

    @GetMapping()
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<UserResponse>> getUsers(){
        List<UsersRecord> users = userService.getUsers();
        return ResponseEntity.ok(UserMapper.fromRecordsToResponses(users));
    }

}
