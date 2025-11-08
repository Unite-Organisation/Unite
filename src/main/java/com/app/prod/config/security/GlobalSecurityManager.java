    package com.app.prod.config.security;

    import com.app.prod.user.enums.UserRole;
    import org.jooq.sources.tables.records.AppUserRecord;

    public interface GlobalSecurityManager {

        boolean currentUserHasRole(String role);

        void checkUserRole(String role);

        AppUserRecord getCurrentUser();

        UserRole getUserRole();

    }
