package com.modelmate.config;

import com.modelmate.user.Role;
import com.modelmate.user.User;
import com.modelmate.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Dev-only: ensures a usable admin account exists so the app can be exercised
 * locally. In production an admin is promoted manually:
 * {@code update users set role = 'ADMIN' where email = ?;}
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevDataInitializer implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "admin@modelmate.local";
    private static final String ADMIN_PASSWORD = "admin12345";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (users.existsByEmailIgnoreCase(ADMIN_EMAIL)) {
            return;
        }
        User admin = new User();
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setEmail(ADMIN_EMAIL);
        admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setRole(Role.ADMIN);
        users.save(admin);
        log.info("Seeded dev admin account {} / {}", ADMIN_EMAIL, ADMIN_PASSWORD);
    }
}
