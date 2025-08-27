package com.app.prod.user.mappers;

import com.app.prod.user.dto.UserRegisterRequest;
import com.app.prod.user.dto.UserResponse;
import com.app.prod.user.enums.UserStatus;
import org.jooq.sources.tables.records.UsersRecord;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.app.prod.config.Constants.BCRYPT_PASSWORD_ENCODER_STRENGTH;

public class UserMapper {

    public static UserResponse fromRecordToResponse(UsersRecord usersRecord){
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

    public static List<UserResponse> fromRecordsToResponses(List<UsersRecord> users){
        List<UserResponse> response = new ArrayList<>();
        for(var user : users){
            response.add(fromRecordToResponse(user));
        }
        return response;
    }

    public static UsersRecord fromRequestToRecord(UserRegisterRequest request, UUID id, LocalDateTime now, UUID selectedRole, BCryptPasswordEncoder encoder){
        return new UsersRecord(
                id,
                request.firstName(),
                request.lastName(),
                request.email(),
                encoder.encode(request.password()),
                selectedRole,
                now,
                request.username(),
                UserStatus.ACTIVE.name()

        );
    }
}
