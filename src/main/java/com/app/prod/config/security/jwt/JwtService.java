package com.app.prod.config.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtUtils jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public String generateJwtAccessToken(String username, String password){
        var auth = new UsernamePasswordAuthenticationToken(username, password);
        Authentication result = authenticationManager.authenticate(auth);
        UserDetails ud = (UserDetails) result.getPrincipal();
        return jwtService.generateToken((org.springframework.security.core.userdetails.User) ud);
    }

    public String generateTokenForExistingUser(String username) {
        UserDetails ud = userDetailsService.loadUserByUsername(username);
        return jwtService.generateToken((org.springframework.security.core.userdetails.User) ud);
    }

    public long getAccessTokenExpiration() {
        return jwtService.getExpiration();
    }
}
