package com.app.prod.config;

import com.app.prod.config.security.GlobalSecurityManager;
import com.app.prod.user.enums.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.AppUserRecord;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static com.app.prod.utils.TestUtils.LOGGED_USER_ID;

@TestConfiguration
@Slf4j
public class SecurityManagerTestConfig {

    @Bean
    public GlobalSecurityManager securityManager() {

        return new GlobalSecurityManager() {
            @Override
            public boolean currentUserHasRole(String role) {
                return true;
            }

            @Override
            public void checkUserRole(String role) {
            }

            @Override
            public AppUserRecord getCurrentUser() {
                AppUserRecord user = new AppUserRecord();
                user.setId(LOGGED_USER_ID);
                return user;
            }

            @Override
            public UserRole getUserRole() {
                log.warn("Returned null as user role for global manager test configuration");
                return null;
            }

        };

    }

}
