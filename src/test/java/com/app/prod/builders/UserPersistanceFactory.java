package com.app.prod.builders;

import com.app.prod.user.enums.UserStatus;
import com.app.prod.user.repository.UserRepository;
import com.app.prod.utils.TestData;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.UsersRecord;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserPersistanceFactory {

    private final Clock clock;
    private final UserRepository userRepository;

    public Builder getNewUser(){ return new Builder(); }

    public class Builder {

        private final UsersRecord instance;

        public Builder() {
            instance = new UsersRecord();
        }

        public Builder firstName(String firstName) {
            instance.setFirstName(firstName);
            return this;
        }

        public Builder lastName(String lastName) {
            instance.setLastName(lastName);
            return this;
        }

        public Builder email(String email) {
            instance.setEmail(email);
            return this;
        }

        public Builder username(String username) {
            instance.setUsername(username);
            return this;
        }

        public Builder password(String password) {
            instance.setPassword(password);
            return this;
        }

        public Builder userRole(UUID userRole) {
            instance.setUserRole(userRole);
            return this;
        }

        public Builder status(String status) {
            instance.setStatus(status);
            return this;
        }

        public Builder buildingId(UUID buildingId) {
            instance.setBuildingId(buildingId);
            return this;
        }

        public Builder createdAt(LocalDateTime now){
            instance.setCreatedAt(now);
            return this;
        }

        public Builder id(UUID id){
            instance.setId(id);
            return this;
        }

        public Builder withRandomValues(){
            instance.setId(UUID.randomUUID());
            instance.setFirstName(TestData.firstName());
            instance.setLastName(TestData.lastName());
            instance.setPassword(TestData.password());
            instance.setUserRole(UUID.fromString("a3f5c9d2-4b8e-4d61-9a67-12c4e9b7f8a1"));
            instance.setStatus(UserStatus.ACTIVE.name());
            instance.setCreatedAt(LocalDateTime.now(clock));
            return this;
        }

        public UsersRecord build() {
            return instance;
        }

        public UsersRecord buildAndSave() {
            UsersRecord record = build();
            userRepository.insertOne(record);
            return record;
        }
    }


}

