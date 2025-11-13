package com.app.prod.config.security;

import com.app.prod.user.enums.UserRole;
import com.app.prod.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.AppUserRecord;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class TokenSecurityManager implements GlobalSecurityManager {

    private final UserService userService;

    @Override
    public boolean currentUserHasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;

        return auth.getAuthorities().stream()
                .anyMatch(granted -> granted.getAuthority().equals(role));
    }

    @Override
    public void checkUserRole(String role) {
        if (!currentUserHasRole(role)) {
            throw new AccessDeniedException("User does not have required role: " + role);
        }
    }

    @Override
    public AppUserRecord getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        String username = auth.getName();
        return userService.findByUsername(username);
    }

    @Override
    public UserRole getUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return null;

        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(UserRole::valueOf)
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("User does not have any role"));
    }

}
