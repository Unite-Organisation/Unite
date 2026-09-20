package com.app.prod.config;

import com.app.prod.user.enums.UserRole;
import com.app.prod.user.enums.UserStatus;
import com.app.prod.user.repository.UserRepository;
import com.app.prod.user.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.AppUserRecord;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserRoleService userRoleService;

    @Override
    public void run(String... args) throws Exception {
        createAdminAccount();
    }

    public void createAdminAccount() {
        UUID adminRoleId = userRoleService.getUserRoleId(UserRole.ADMIN);

        if (userRepository.adminExists(adminRoleId)) { return; }

        AppUserRecord admin = new AppUserRecord();
        admin.setFirstName("Dwight");
        admin.setLastName("Schrute");
        admin.setEmail("unite@gmail.com");
        admin.setUsername("Admin");
        admin.setPassword("$2a$10$xT0LORpan3WEIsWLfbEOtuuLXyL4l/vRGoTVCSRgF7t9iOWt0rkXq");
        admin.setUserRole(adminRoleId);
        admin.setStatus(UserStatus.ACTIVE.name());

        userRepository.insertOne(admin);
    }
}
