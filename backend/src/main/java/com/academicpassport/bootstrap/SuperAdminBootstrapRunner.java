package com.academicpassport.bootstrap;

import com.academicpassport.auth.User;
import com.academicpassport.auth.UserRepository;
import com.academicpassport.auth.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SuperAdminBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SuperAdminBootstrapProperties properties;

    public SuperAdminBootstrapRunner(UserRepository userRepository,
                                     PasswordEncoder passwordEncoder,
                                     SuperAdminBootstrapProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        if (!properties.enabled()) {
            log.info("Super Admin bootstrap is disabled.");
            return;
        }

        if (!StringUtils.hasText(properties.email()) || !StringUtils.hasText(properties.password())) {
            throw new IllegalStateException("Super Admin bootstrap is ENABLED, but email or password is missing or blank. Startup failed.");
        }

        if (userRepository.existsByRole(UserRole.SUPER_ADMIN)) {
            log.info("A SUPER_ADMIN already exists in the system. Bootstrap skipped.");
            return;
        }

        log.info("No SUPER_ADMIN found. Bootstrapping initial super admin account...");

        String normalizedEmail = properties.email().trim().toLowerCase();

        User superAdmin = new User();
        superAdmin.setEmail(normalizedEmail);
        superAdmin.setPasswordHash(passwordEncoder.encode(properties.password()));
        superAdmin.setRole(UserRole.SUPER_ADMIN);
        superAdmin.setIsActive(true);
        superAdmin.setIsVerified(true);
        // DB constraint chk_super_admin_no_college demands this is null
        superAdmin.setCollege(null);

        userRepository.save(superAdmin);

        log.info("Initial SUPER_ADMIN account bootstrapped successfully for: {}", normalizedEmail);
    }
}
