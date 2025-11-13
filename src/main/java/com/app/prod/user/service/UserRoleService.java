package com.app.prod.user.service;

import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.user.enums.UserRole;
import com.app.prod.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.UserRoleRecord;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;

    public UUID getUserRoleId(UserRole userRole){
        return userRoleRepository.findByRoleName(userRole)
                .map(UserRoleRecord::getId)
                .orElseThrow(
                    () -> new EntityNotPresentException(
                            String.format("Role %s not found", userRole.name()),
                            org.jooq.sources.tables.UserRole.class.getSimpleName()
                    )
                );
    }

    public UserRole getUserRoleFromId(UUID id){
        return userRoleRepository.findById(id)
                .map(UserRoleRecord::getUserRole)
                .map(UserRole::valueOf)
                .orElseThrow(
                    () -> new EntityNotPresentException(
                            String.format("Role with id %s not found", id),
                            org.jooq.sources.tables.UserRole.class.getSimpleName()
                    )
            );
    }

}
