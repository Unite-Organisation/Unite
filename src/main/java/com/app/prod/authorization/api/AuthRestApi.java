package com.app.prod.authorization.api;

import com.app.prod.user.dto.UserLoginRequest;
import com.app.prod.user.dto.UserRegisterRequest;
import com.app.prod.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthRestApi {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody UserRegisterRequest request) {
        userService.register(request);
        return ResponseEntity.ok().body("User registered");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody UserLoginRequest request) {
        String token = userService.login(request);
        return ResponseEntity.ok(String.format("Token: %s", token));
    }
}
