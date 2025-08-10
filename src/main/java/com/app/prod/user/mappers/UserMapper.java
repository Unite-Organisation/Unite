package com.app.prod.user.mappers;

import com.app.prod.user.dto.UserRequest;
import com.app.prod.user.dto.UserResponse;
import org.jooq.sources.tables.records.UsersRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.app.prod.config.Constants.BCRYPT_PASSWORD_ENCODER_STRENGTH;

public class UserMapper {

    private static final BCryptPasswordEncoder encoder =
            new BCryptPasswordEncoder(BCRYPT_PASSWORD_ENCODER_STRENGTH);

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

    public static UsersRecord fromRequestToRecord(UserRequest request, UUID id, LocalDateTime now){
        return new UsersRecord(
                id,
                request.firstName(),
                request.lastName(),
                request.email(),
                encoder.encode(request.password()),
                request.role(),
                now,
                request.username()

        );
    }
}
