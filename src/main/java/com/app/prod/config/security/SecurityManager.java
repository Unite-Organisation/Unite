package com.app.prod.config.security;

import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.exceptions.exceptions.FailedFetchingLoggedUserException;
import com.app.prod.exceptions.exceptions.PermissionDeniedException;
import com.app.prod.user.enums.UserRole;
import com.app.prod.user.repository.UserRepository;
import com.app.prod.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.UsersRecord;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SecurityManager {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    private Optional<String> getLoggedUsername(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication == null || !authentication.isAuthenticated())
            return Optional.empty();

        String username = null;

        Object principal = authentication.getPrincipal();
        username = ((UserDetails) principal).getUsername();

        if (username == null) {
            return Optional.empty();
        }

        return Optional.of(username);
    }

    public Optional<UsersRecord> getLoggedUser(){
        String username = getLoggedUsername().orElseThrow(
                () -> new FailedFetchingLoggedUserException("Cannot fetch logged user.")
        );
        return userRepository.findByUsername(username);
    }

    public boolean userIs(UserRole role){
        var user = getLoggedUser().orElseThrow(
                () -> new EntityNotPresentException("User doesn't exist.")
        );

        var userRole = userRoleRepository.findById(user.getUserRole()).orElseThrow(
                () -> new EntityNotPresentException("Role doesn't exist.")
        );

        return(UserRole.fromString(userRole.getUserRole()) == role);
    }

    public void checkUserPermission(UserRole role){
        if(!userIs(role)){
            throw new PermissionDeniedException("User is not permitted to use this resource");
        }
    }

}
