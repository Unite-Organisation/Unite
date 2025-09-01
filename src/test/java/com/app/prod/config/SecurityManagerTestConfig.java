package com.app.prod.config;

import com.app.prod.config.security.GlobalSecurityManager;
import org.jooq.sources.tables.records.UsersRecord;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static com.app.prod.utils.TestUtils.LOGGED_USER_ID;

@TestConfiguration
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
            public UsersRecord getCurrentUser() {
                UsersRecord user = new UsersRecord();
                user.setId(LOGGED_USER_ID);
                return user;
            }
        };

    }

}
