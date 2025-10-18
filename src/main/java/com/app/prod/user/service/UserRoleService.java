package com.app.prod.user.service;

import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.user.enums.UserRole;
import com.app.prod.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.UserRoles;
import org.jooq.sources.tables.records.UserRolesRecord;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;

    public UUID getUserRoleId(UserRole userRole){
        return userRoleRepository.findByRoleName(userRole)
                .map(UserRolesRecord::getId)
                .orElseThrow(
                    () -> new EntityNotPresentException(
                            String.format("Role %s not found", userRole.name()),
                            UserRoles.class.getSimpleName()
                    )
                );
    }

}
