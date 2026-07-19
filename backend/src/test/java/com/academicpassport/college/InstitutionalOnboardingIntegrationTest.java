package com.academicpassport.college;

import com.academicpassport.auth.TokenGeneratorUtil;
import com.academicpassport.auth.User;
import com.academicpassport.auth.UserRepository;
import com.academicpassport.auth.UserRole;
import com.academicpassport.college.dto.CreateInvitationRequest;
import com.academicpassport.college.dto.OnboardingRequest;
import com.academicpassport.config.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class InstitutionalOnboardingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private InstitutionalOnboardingService onboardingService;

    @Autowired
    private CollegeInvitationRepository invitationRepository;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User superAdmin;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE users, students, staff, subjects, semesters, departments, colleges, college_invitations RESTART IDENTITY CASCADE");

        superAdmin = new User();
        superAdmin.setEmail("super@test.com");
        superAdmin.setPasswordHash("hashed_pw");
        superAdmin.setRole(UserRole.SUPER_ADMIN);
        superAdmin.setIsActive(true);
        superAdmin.setIsVerified(true);
        userRepository.save(superAdmin);
    }

    @Test
    void testGenerateAndValidateInvitation() {
        CreateInvitationRequest req = new CreateInvitationRequest();
        req.setInstitutionName("Test University");
        req.setAdminEmail("admin@testuniversity.edu");

        String rawToken = onboardingService.generateInvitation(req, superAdmin);
        assertThat(rawToken).isNotBlank();

        // Validate
        var dto = onboardingService.validateInvitation(rawToken);
        assertThat(dto.getInstitutionName()).isEqualTo("Test University");
        assertThat(dto.getAdminEmail()).isEqualTo("admin@testuniversity.edu");
    }

    @Test
    void testFinalizeOnboardingConsumesTokenAtomically() throws InterruptedException {
        CreateInvitationRequest req = new CreateInvitationRequest();
        req.setInstitutionName("Atomic University");
        req.setAdminEmail("atomic@test.edu");

        String rawToken = onboardingService.generateInvitation(req, superAdmin);

        OnboardingRequest onboardReq = new OnboardingRequest();
        onboardReq.setInstitutionName("Atomic University");
        onboardReq.setAddressLine("123 Atomic Ave");
        onboardReq.setCity("Atom City");
        onboardReq.setState("AT");
        onboardReq.setPostalCode("12345");
        onboardReq.setCountry("USA");
        onboardReq.setContactName("Dr. Atom");
        onboardReq.setContactEmail("atomic@test.edu");
        onboardReq.setContactPhone("555-1234");
        onboardReq.setPassword("SecurePass123!");

        // Simulate concurrent requests
        int threadCount = 3;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    latch.await();
                    onboardingService.finalizeOnboarding(onboardReq, rawToken);
                    successCount.incrementAndGet();
                } catch (IllegalArgumentException e) {
                    failCount.incrementAndGet(); // Expected for losing threads
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        latch.countDown(); // start all threads
        doneLatch.await(); // wait for all to finish

        // EXACTLY ONE should succeed
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(threadCount - 1);

        // Assert DB state
        assertThat(collegeRepository.count()).isEqualTo(1);
        College college = collegeRepository.findAll().get(0);
        assertThat(college.getCollegeCode()).startsWith("AP-");
        assertThat(college.getContactEmail()).isEqualTo("atomic@test.edu");

        // The user was created
        var adminList = userRepository.findByEmail("atomic@test.edu");
        assertThat(adminList).isNotEmpty();
        User admin = adminList.get(0);
        assertThat(admin.getRole()).isEqualTo(UserRole.UNIVERSITY_ADMIN);
        assertThat(admin.getCollege().getId()).isEqualTo(college.getId());
    }
}
