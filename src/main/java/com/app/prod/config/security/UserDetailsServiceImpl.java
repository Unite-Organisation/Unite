package com.app.prod.config.security;

import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.user.repository.UserRepository;
import com.app.prod.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.AppUserRecord;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var userRecord = userRepository.findByUsername(username).orElseThrow(
                () -> new UsernameNotFoundException(String.format("User %s not found", username))
        );

        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + getUserRole(userRecord))
        );

        log.debug("Loaded user {} with password {} and authorities {}",
                userRecord.getUsername(),
                userRecord.getPassword(),
                authorities);

        return new org.springframework.security.core.userdetails.User(
                userRecord.getUsername(), userRecord.getPassword(), authorities
        );
    }

    private String getUserRole(AppUserRecord user){
        return userRoleRepository.findById(user.getUserRole()).orElseThrow(
                () -> new EntityNotPresentException(
                        String.format("User role with id %s not found.", user.getUserRole()),
                        org.jooq.sources.tables.UserRole.class.getSimpleName()
                )
        ).getUserRole();

    }
}
