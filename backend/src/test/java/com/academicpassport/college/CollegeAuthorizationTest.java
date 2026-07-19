package com.academicpassport.college;

import com.academicpassport.auth.User;
import com.academicpassport.auth.UserPrincipal;
import com.academicpassport.auth.UserRepository;
import com.academicpassport.auth.UserRole;
import com.academicpassport.config.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class CollegeAuthorizationTest extends AbstractIntegrationTest {

    @Autowired
    private CollegeController collegeController;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User student;
    private User staff;
    private User universityAdmin;
    private User superAdmin;

    @BeforeEach
    void setUp() {
        userRepository.deleteAllInBatch();
        collegeRepository.deleteAllInBatch();
        SecurityContextHolder.clearContext();

        College college = new College();
        college.setName("Auth Test College");
        college.setCollegeCode("ATC-123");
        college.setIsActive(true);
        college = collegeRepository.save(college);

        student = new User();
        student.setEmail("student@test.com");
        student.setPasswordHash(passwordEncoder.encode("password"));
        student.setRole(UserRole.STUDENT);
        student.setCollege(college);
        student.setIsActive(true);
        student.setIsVerified(true);
        userRepository.save(student);

        staff = new User();
        staff.setEmail("staff@test.com");
        staff.setPasswordHash(passwordEncoder.encode("password"));
        staff.setRole(UserRole.STAFF);
        staff.setCollege(college);
        staff.setIsActive(true);
        staff.setIsVerified(true);
        userRepository.save(staff);

        universityAdmin = new User();
        universityAdmin.setEmail("admin@test.com");
        universityAdmin.setPasswordHash(passwordEncoder.encode("password"));
        universityAdmin.setRole(UserRole.UNIVERSITY_ADMIN);
        universityAdmin.setCollege(college);
        universityAdmin.setIsActive(true);
        universityAdmin.setIsVerified(true);
        userRepository.save(universityAdmin);
        
        superAdmin = new User();
        superAdmin.setEmail("superadmin@test.com");
        superAdmin.setPasswordHash(passwordEncoder.encode("password"));
        superAdmin.setRole(UserRole.SUPER_ADMIN);
        superAdmin.setIsActive(true);
        superAdmin.setIsVerified(true);
        userRepository.save(superAdmin);
    }
    
    private void authenticate(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void studentCannotAccessCollegeEndpoints() {
        authenticate(student);
        assertThatThrownBy(() -> collegeController.searchColleges(null, null, 0, 20))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void staffCannotAccessCollegeEndpoints() {
        authenticate(staff);
        assertThatThrownBy(() -> collegeController.searchColleges(null, null, 0, 20))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void universityAdminCannotAccessSuperAdminCollegeEndpoints() {
        authenticate(universityAdmin);
        assertThatThrownBy(() -> collegeController.searchColleges(null, null, 0, 20))
                .isInstanceOf(AccessDeniedException.class);
    }
    
    @Test
    void superAdminCanAccessCollegeEndpoints() {
        authenticate(superAdmin);
        collegeController.searchColleges(null, null, 0, 20); // Should not throw
    }
}
