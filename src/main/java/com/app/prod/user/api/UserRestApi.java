package com.app.prod.user.api;

import com.app.prod.user.dto.UserLoginRequest;
import com.app.prod.user.dto.UserRequest;
import com.app.prod.user.dto.UserResponse;
import com.app.prod.user.mappers.UserMapper;
import com.app.prod.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.UsersRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("user")
@RequiredArgsConstructor
public class UserRestApi {

    private final UserService userService;

    @GetMapping()
    public ResponseEntity<List<UserResponse>> getUsers(){
        List<UsersRecord> users = userService.getUsers();
        return ResponseEntity.ok(UserMapper.fromRecordsToResponses(users));
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody UserRequest request){
        String responseMessage = userService.register(request);
        return ResponseEntity.ok(responseMessage);
    }

//    @PostMapping("/login")
//    public ResponseEntity<String> loginUser(@RequestBody UserLoginRequest request){
//        String responseMessage = userService.verify(request);
//        return ResponseEntity.ok(responseMessage);
//    }

}
