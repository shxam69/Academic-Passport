package com.academicpassport.student;

import com.academicpassport.auth.User;
import com.academicpassport.auth.UserRepository;
import com.academicpassport.auth.UserRole;
import com.academicpassport.college.College;
import com.academicpassport.college.CollegeRepository;
import com.academicpassport.college.Department;
import com.academicpassport.college.DepartmentRepository;
import com.academicpassport.config.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StudentRepositoryTest extends AbstractIntegrationTest {

    @Autowired private StudentRepository studentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CollegeRepository collegeRepository;
    @Autowired private DepartmentRepository departmentRepository;

    private College collegeA;
    private College collegeB;
    private Department departmentA;

    @BeforeEach
    void setUp() {
        collegeA = collegeRepository.saveAndFlush(newCollege("College A"));
        collegeB = collegeRepository.saveAndFlush(newCollege("College B"));
        departmentA = departmentRepository.saveAndFlush(newDepartment(collegeA, "CSE"));
    }

    @Test
    void findByIdAndCollegeId_returnsEmpty_whenCollegeDoesNotMatch() {
        Student student = persistStudent(collegeA, departmentA, "RG-" + UUID.randomUUID().toString().substring(0, 8));

        // This is the concrete IDOR-prevention mechanism from the RBAC review,
        // now expressed as a repository contract: a student record fetched with
        // the WRONG tenant id must come back empty, not just "found but you're
        // not authorized" — the query itself must not leak the row's existence
        // across tenants.
        Optional<Student> wrongTenant = studentRepository.findByIdAndCollegeId(student.getId(), collegeB.getId());
        Optional<Student> rightTenant = studentRepository.findByIdAndCollegeId(student.getId(), collegeA.getId());

        assertThat(wrongTenant).isEmpty();
        assertThat(rightTenant).isPresent();
    }

    @Test
    void universityRegisterNo_rejectsDuplicate_evenAcrossDifferentColleges() {
        // Deliberately global uniqueness (not per-college) — see the architecture
        // review: a university register number is issued by the university, not
        // by this platform, so a duplicate is a real-world data problem worth
        // catching regardless of which college record it shows up under.
        String registerNo = "GLB-" + UUID.randomUUID().toString().substring(0, 8);
        persistStudent(collegeA, departmentA, registerNo);

        Department departmentB = departmentRepository.saveAndFlush(newDepartment(collegeB, "ECE"));

        assertThatThrownBy(() -> persistStudent(collegeB, departmentB, registerNo))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void concurrentEdits_secondWriterFailsOptimisticLock() {
        Student student = persistStudent(collegeA, departmentA, "OP-" + UUID.randomUUID().toString().substring(0, 8));

        // Simulate two concurrent editors (e.g. self-service edit racing an admin
        // correction) each loading their own copy of the same row.
        Student copy1 = studentRepository.findById(student.getId()).orElseThrow();
        Student copy2 = studentRepository.findById(student.getId()).orElseThrow();

        copy1.setSection("A");
        studentRepository.saveAndFlush(copy1);

        copy2.setSection("B");
        // copy2 still has the pre-update version number — this is exactly the
        // lost-update scenario @Version exists to prevent.
        assertThatThrownBy(() -> studentRepository.saveAndFlush(copy2))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    private Student persistStudent(College college, Department department, String registerNo) {
        User user = new User();
        user.setCollege(college);
        user.setEmail("stu-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com");
        user.setPasswordHash("irrelevant-for-this-test");
        user.setRole(UserRole.STUDENT);
        user = userRepository.saveAndFlush(user);

        Student student = new Student();
        student.setUser(user);
        student.setDepartment(department);
        student.setCollege(college);
        student.setFullName("Test Student");
        student.setRollNumber("R-" + UUID.randomUUID().toString().substring(0, 8));
        student.setUniversityRegisterNo(registerNo);
        student.setDob(LocalDate.of(2003, 1, 1));
        student.setBatchYear(2022);
        return studentRepository.saveAndFlush(student);
    }

    private College newCollege(String name) {
        College college = new College();
        college.setName(name);
        college.setCollegeCode("C-" + UUID.randomUUID().toString().substring(0, 8));
        return college;
    }

    private Department newDepartment(College college, String code) {
        Department department = new Department();
        department.setCollege(college);
        department.setName(code + " Department");
        department.setCode("D-" + UUID.randomUUID().toString().substring(0, 8));
        return department;
    }
}
