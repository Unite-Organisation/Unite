package com.app.prod.user.service;

import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
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
                .orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.USER_ROLE_NOT_FOUND)));
    }

    public UserRole getUserRoleFromId(UUID id){
        return userRoleRepository.findById(id)
                .map(UserRoleRecord::getUserRole)
                .map(UserRole::valueOf)
                .orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.USER_ROLE_NOT_FOUND)));
    }

}
