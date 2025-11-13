package com.app.prod.user.mappers;

import com.app.prod.user.dto.UserRegisterRequest;
import com.app.prod.user.dto.UserResponse;
import com.app.prod.user.enums.UserStatus;
import org.jooq.sources.tables.records.AppUserRecord;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.app.prod.config.Constants.BCRYPT_PASSWORD_ENCODER_STRENGTH;

public class UserMapper {

    public static UserResponse fromRecordToResponse(AppUserRecord usersRecord){
        return new UserResponse(
                usersRecord.getId(),
                usersRecord.getFirstName(),
                usersRecord.getLastName(),
                usersRecord.getEmail(),
                usersRecord.getPassword(),
                usersRecord.getUserRole().toString(),
                usersRecord.getCreatedAt()
        );
    }

    public static List<UserResponse> fromRecordsToResponses(List<AppUserRecord> users){
        List<UserResponse> response = new ArrayList<>();
        for(var user : users){
            response.add(fromRecordToResponse(user));
        }
        return response;
    }

    public static AppUserRecord fromRequestToRecord(UserRegisterRequest request, UUID id, LocalDateTime now, UUID selectedRole, BCryptPasswordEncoder encoder){
        return new AppUserRecord(
                id,
                request.firstName(),
                request.lastName(),
                request.email(),
                request.username(),
                encoder.encode(request.password()),
                selectedRole,
                UserStatus.ACTIVE.name(),
                now,
                null
        );
    }
}
