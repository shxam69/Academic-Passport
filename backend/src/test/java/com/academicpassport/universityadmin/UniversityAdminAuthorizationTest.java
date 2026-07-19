package com.academicpassport.universityadmin;

import com.academicpassport.auth.User;
import com.academicpassport.auth.UserPrincipal;
import com.academicpassport.auth.UserRepository;
import com.academicpassport.auth.UserRole;
import com.academicpassport.college.College;
import com.academicpassport.college.CollegeRepository;
import com.academicpassport.college.Department;
import com.academicpassport.college.DepartmentRepository;
import com.academicpassport.college.SemesterRepository;
import com.academicpassport.college.SubjectRepository;
import com.academicpassport.staff.StaffRepository;
import com.academicpassport.student.StudentRepository;
import com.academicpassport.config.AbstractIntegrationTest;
import com.academicpassport.universityadmin.dto.CreateDepartmentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class UniversityAdminAuthorizationTest extends AbstractIntegrationTest {

    @Autowired
    private UniversityAdminController controller;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User student;
    private User staff;
    private User superAdmin;
    private User adminA;
    private User adminB;
    
    private College collegeA;
    private College collegeB;
    private Department deptB;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE users, students, staff, subjects, semesters, departments, colleges RESTART IDENTITY CASCADE");
        SecurityContextHolder.clearContext();

        collegeA = new College();
        collegeA.setName("College A");
        collegeA.setCollegeCode("CA-001");
        collegeA.setIsActive(true);
        collegeA = collegeRepository.save(collegeA);

        collegeB = new College();
        collegeB.setName("College B");
        collegeB.setCollegeCode("CB-001");
        collegeB.setIsActive(true);
        collegeB = collegeRepository.save(collegeB);

        deptB = new Department();
        deptB.setCollege(collegeB);
        deptB.setName("Science");
        deptB.setCode("SCI");
        deptB = departmentRepository.save(deptB);

        adminA = createUser("adminA@test.com", UserRole.UNIVERSITY_ADMIN, collegeA);
        adminB = createUser("adminB@test.com", UserRole.UNIVERSITY_ADMIN, collegeB);
        student = createUser("student@test.com", UserRole.STUDENT, collegeA);
        staff = createUser("staff@test.com", UserRole.STAFF, collegeA);
        superAdmin = createUser("sa@test.com", UserRole.SUPER_ADMIN, null);
    }

    private User createUser(String email, UserRole role, College college) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setCollege(college);
        user.setIsActive(true);
        user.setIsVerified(true);
        return userRepository.save(user);
    }
    
    private UserPrincipal authenticate(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        return principal;
    }

    @Test
    void otherRolesCannotAccessUniversityAdminEndpoints() {
        authenticate(student);
        assertThatThrownBy(() -> controller.getDepartments(new UserPrincipal(student), null, 0, 10))
                .isInstanceOf(AccessDeniedException.class);

        authenticate(staff);
        assertThatThrownBy(() -> controller.getDepartments(new UserPrincipal(staff), null, 0, 10))
                .isInstanceOf(AccessDeniedException.class);

        authenticate(superAdmin);
        assertThatThrownBy(() -> controller.getDepartments(new UserPrincipal(superAdmin), null, 0, 10))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void adminACannotAccessCollegeBData() {
        UserPrincipal principal = authenticate(adminA);

        // Try to update Department B
        CreateDepartmentRequest req = new CreateDepartmentRequest();
        req.setName("Hack");
        req.setCode("HCK");

        assertThatThrownBy(() -> controller.updateDepartment(principal, deptB.getId(), null))
                .hasMessageContaining("Department not found"); // 404 behavior

        // Try to delete Department B
        assertThatThrownBy(() -> controller.deleteDepartment(principal, deptB.getId()))
                .hasMessageContaining("Department not found");
    }

    @Test
    void adminACanAccessOwnCollegeData() {
        UserPrincipal principal = authenticate(adminA);
        
        CreateDepartmentRequest req = new CreateDepartmentRequest();
        req.setName("Arts");
        req.setCode("ART");
        
        // Create
        var createResp = controller.createDepartment(principal, req);
        assertThat(createResp.getStatusCode().is2xxSuccessful()).isTrue();
        Long deptId = createResp.getBody().getId();
        
        // Read
        var getResp = controller.getDepartments(principal, null, 0, 10);
        assertThat(getResp.getBody().getContent()).hasSize(1);
        
        // Delete
        var delResp = controller.deleteDepartment(principal, deptId);
        assertThat(delResp.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
