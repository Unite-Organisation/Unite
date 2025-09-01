package com.app.prod.config.security;

import org.jooq.sources.tables.records.UsersRecord;

public interface GlobalSecurityManager {

    boolean currentUserHasRole(String role);

    void checkUserRole(String role);

    public UsersRecord getCurrentUser();
}
