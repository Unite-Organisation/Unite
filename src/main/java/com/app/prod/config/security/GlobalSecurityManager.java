    package com.app.prod.config.security;

    import com.app.prod.user.enums.UserRole;
    import org.jooq.sources.tables.records.UsersRecord;

    public interface GlobalSecurityManager {

        boolean currentUserHasRole(String role);

        void checkUserRole(String role);

        UsersRecord getCurrentUser();

        UserRole getUserRole();

    }
