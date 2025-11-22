package com.app.prod.authorization.api;

import com.app.prod.authorization.dto.TokenResponse;
import com.app.prod.user.dto.UserActivateRequest;
import com.app.prod.user.dto.UserLoginRequest;
import com.app.prod.user.dto.UserRegisterRequest;
import com.app.prod.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(name = "App authorization")
public class AuthRestApi {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody UserRegisterRequest request) {
        userService.register(request);
        return ResponseEntity.ok().body("User registered");
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody UserLoginRequest request) {
        String token = userService.login(request);
        var tokenResponse = new TokenResponse(token);
        return ResponseEntity.ok(tokenResponse);
    }

    @PutMapping("/activate")
    public ResponseEntity<TokenResponse> activate(@RequestBody UserActivateRequest request){
        String token = userService.activate(request);
        var tokenResponse = new TokenResponse(token);
        return ResponseEntity.ok(tokenResponse);
    }
}
