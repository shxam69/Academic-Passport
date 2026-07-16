package com.academicpassport.bootstrap;

import com.academicpassport.auth.User;
import com.academicpassport.auth.UserRepository;
import com.academicpassport.auth.UserRole;
import com.academicpassport.config.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SuperAdminBootstrapRunnerTest extends AbstractIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Clean up any SUPER_ADMIN created by tests, but use native query to truly delete them
        // because we want a blank slate for bootstrap testing, bypassing soft deletes.
        userRepository.deleteAllInBatch();
    }

    @Test
    void createsFirstSuperAdmin_whenEnabledAndValidConfig() {
        SuperAdminBootstrapProperties props = new SuperAdminBootstrapProperties(true, "admin@test.com", "Secret123!");
        SuperAdminBootstrapRunner runner = new SuperAdminBootstrapRunner(userRepository, passwordEncoder, props);

        runner.run();

        List<User> superAdmins = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.SUPER_ADMIN)
                .toList();

        assertThat(superAdmins).hasSize(1);
        User admin = superAdmins.get(0);
        assertThat(admin.getEmail()).isEqualTo("admin@test.com");
        assertThat(admin.getCollege()).isNull(); // Must be null by DB constraint
        assertThat(admin.getIsActive()).isTrue();
        
        // Ensure password is not stored raw
        assertThat(admin.getPasswordHash()).isNotEqualTo("Secret123!");
        // Ensure it's a valid BCrypt hash
        assertThat(passwordEncoder.matches("Secret123!", admin.getPasswordHash())).isTrue();
    }

    @Test
    void skipsBootstrap_whenDisabled() {
        SuperAdminBootstrapProperties props = new SuperAdminBootstrapProperties(false, "admin@test.com", "Secret123!");
        SuperAdminBootstrapRunner runner = new SuperAdminBootstrapRunner(userRepository, passwordEncoder, props);

        runner.run();

        assertThat(userRepository.existsByRole(UserRole.SUPER_ADMIN)).isFalse();
    }

    @Test
    void throwsException_whenEnabledButMissingEmailOrPassword() {
        SuperAdminBootstrapProperties propsMissingEmail = new SuperAdminBootstrapProperties(true, "", "Secret123!");
        SuperAdminBootstrapRunner runner1 = new SuperAdminBootstrapRunner(userRepository, passwordEncoder, propsMissingEmail);

        assertThatThrownBy(() -> runner1.run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing or blank");

        SuperAdminBootstrapProperties propsMissingPassword = new SuperAdminBootstrapProperties(true, "admin@test.com", null);
        SuperAdminBootstrapRunner runner2 = new SuperAdminBootstrapRunner(userRepository, passwordEncoder, propsMissingPassword);

        assertThatThrownBy(() -> runner2.run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing or blank");
    }

    @Test
    void idempotency_doesNotCreateDuplicates_andDoesNotOverwritePassword() {
        // First boot
        SuperAdminBootstrapProperties props1 = new SuperAdminBootstrapProperties(true, "admin@test.com", "Secret123!");
        SuperAdminBootstrapRunner runner1 = new SuperAdminBootstrapRunner(userRepository, passwordEncoder, props1);
        runner1.run();

        List<User> superAdmins = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.SUPER_ADMIN)
                .toList();
        assertThat(superAdmins).hasSize(1);
        String originalHash = superAdmins.get(0).getPasswordHash();

        // Second boot (simulate restart) with a DIFFERENT configured password
        SuperAdminBootstrapProperties props2 = new SuperAdminBootstrapProperties(true, "admin@test.com", "NewPassword999!");
        SuperAdminBootstrapRunner runner2 = new SuperAdminBootstrapRunner(userRepository, passwordEncoder, props2);
        runner2.run();

        // Still only 1
        superAdmins = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.SUPER_ADMIN)
                .toList();
        assertThat(superAdmins).hasSize(1);

        // Password hash must NOT be changed!
        assertThat(superAdmins.get(0).getPasswordHash()).isEqualTo(originalHash);
        assertThat(passwordEncoder.matches("Secret123!", superAdmins.get(0).getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("NewPassword999!", superAdmins.get(0).getPasswordHash())).isFalse();
    }
}
