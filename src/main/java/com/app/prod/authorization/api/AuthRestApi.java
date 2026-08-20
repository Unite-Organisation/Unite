package com.app.prod.authorization.api;

import com.app.prod.authorization.dto.AuthTokensDto;
import com.app.prod.authorization.dto.TokenResponse;
import com.app.prod.authorization.service.AuthenticationService;
import com.app.prod.user.dto.UserActivateRequest;
import com.app.prod.user.dto.UserLoginRequest;
import com.app.prod.user.dto.UserRegisterRequest;
import com.app.prod.user.service.UserService;
import com.app.prod.utils.ApplicationInfo;
import com.app.prod.utils.HttpUtils;
import com.app.prod.utils.SimpleResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(name = "App authorization")
public class AuthRestApi {

    private final UserService userService;
    private final AuthenticationService authenticationService;
    private final ApplicationInfo applicationInfo;

    @PostMapping("/register")
    public ResponseEntity<SimpleResponse> register(@RequestBody UserRegisterRequest request) {
        userService.register(request);
        return ResponseEntity.ok().body(new SimpleResponse("User registered"));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody UserLoginRequest request, HttpServletRequest httpRequest) {
        String ipAddress = HttpUtils.getClientIp(httpRequest);
        String userAgent = HttpUtils.getUserAgent(httpRequest);

        AuthTokensDto authTokens = authenticationService.getTokensWithoutRefreshingAndRotating(request.username(), request.password(), ipAddress, userAgent);
        ResponseCookie cookie = prepareRefreshTokenCookie(authTokens.refreshToken(), authenticationService.getRefreshTokenStandardDuration());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new TokenResponse(authTokens.accessToken()));
    }

    @PostMapping("logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "refreshToken", required = false) String refreshToken, HttpServletRequest httpRequest) {
        authenticationService.deactivateRefreshToken(refreshToken);
        ResponseCookie cookie = prepareRefreshTokenCookie("", Duration.ZERO);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@CookieValue(name = "refreshToken") String refreshToken, HttpServletRequest httpRequest) {
        String ipAddress = HttpUtils.getClientIp(httpRequest);
        String userAgent = HttpUtils.getUserAgent(httpRequest);

        AuthTokensDto authTokens = authenticationService.getTokensRefreshRotate(refreshToken, ipAddress, userAgent);
        ResponseCookie cookie = prepareRefreshTokenCookie(authTokens.refreshToken(), authenticationService.getRefreshTokenStandardDuration());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new TokenResponse(authTokens.accessToken()));
    }

    @PutMapping("/activate")
    public ResponseEntity<TokenResponse> activate(@Valid @RequestBody UserActivateRequest request){
        String token = userService.activate(request);
        var tokenResponse = new TokenResponse(token);
        return ResponseEntity.ok(tokenResponse);
    }

    private ResponseCookie prepareRefreshTokenCookie(String refreshToken, Duration cookieMaxAge) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(applicationInfo.isProdEnvironment())
                .path("/")
                .maxAge(cookieMaxAge)
                .sameSite("Strict")
                .build();
    }
}
