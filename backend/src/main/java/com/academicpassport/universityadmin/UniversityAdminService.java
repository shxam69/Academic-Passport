package com.academicpassport.universityadmin;

import com.academicpassport.auth.User;
import com.academicpassport.auth.UserPrincipal;
import com.academicpassport.auth.UserRepository;
import com.academicpassport.auth.UserRole;
import com.academicpassport.college.College;
import com.academicpassport.college.CollegeRepository;
import com.academicpassport.college.Department;
import com.academicpassport.college.DepartmentRepository;
import com.academicpassport.college.Semester;
import com.academicpassport.college.SemesterRepository;
import com.academicpassport.college.Subject;
import com.academicpassport.college.SubjectRepository;
import com.academicpassport.common.ResourceNotFoundException;
import com.academicpassport.staff.Staff;
import com.academicpassport.staff.StaffRepository;
import com.academicpassport.student.Student;
import com.academicpassport.student.StudentRepository;
import com.academicpassport.universityadmin.dto.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UniversityAdminService {

    private final CollegeRepository collegeRepository;
    private final DepartmentRepository departmentRepository;
    private final SemesterRepository semesterRepository;
    private final SubjectRepository subjectRepository;
    private final StaffRepository staffRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UniversityAdminService(
            CollegeRepository collegeRepository,
            DepartmentRepository departmentRepository,
            SemesterRepository semesterRepository,
            SubjectRepository subjectRepository,
            StaffRepository staffRepository,
            StudentRepository studentRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.collegeRepository = collegeRepository;
        this.departmentRepository = departmentRepository;
        this.semesterRepository = semesterRepository;
        this.subjectRepository = subjectRepository;
        this.staffRepository = staffRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public DashboardStatsDto getDashboardStats(UserPrincipal principal) {
        long totalDepartments = departmentRepository.findAllByCollegeId(principal.getCollegeId()).size();
        // Since we didn't add count methods, we can just fetch all or write a specific count method later.
        // For MVP, size is fine. 
        long totalStaff = staffRepository.searchStaff(principal.getCollegeId(), null, null, Pageable.unpaged()).getTotalElements();
        long totalStudents = studentRepository.searchStudents(principal.getCollegeId(), null, null, Pageable.unpaged()).getTotalElements();
        
        DashboardStatsDto stats = new DashboardStatsDto();
        stats.setTotalDepartments(totalDepartments);
        stats.setTotalStaff(totalStaff);
        stats.setTotalStudents(totalStudents);
        return stats;
    }

    // --- Department ---

    public PageResponse<DepartmentDto> getDepartments(UserPrincipal principal, String search, Pageable pageable) {
        Page<Department> page = departmentRepository.searchDepartments(principal.getCollegeId(), search, pageable);
        return new PageResponse<>(page.map(this::mapToDepartmentDto));
    }

    @Transactional
    public DepartmentDto createDepartment(UserPrincipal principal, CreateDepartmentRequest request) {
        College college = collegeRepository.findById(principal.getCollegeId())
                .orElseThrow(() -> new ResourceNotFoundException("College not found"));

        Department department = new Department();
        department.setCollege(college);
        department.setName(request.getName());
        department.setCode(request.getCode());

        try {
            department = departmentRepository.save(department);
            return mapToDepartmentDto(department);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Department with this code already exists in your college");
        }
    }

    @Transactional
    public DepartmentDto updateDepartment(UserPrincipal principal, Long id, UpdateDepartmentRequest request) {
        Department department = departmentRepository.findByIdAndCollegeId(id, principal.getCollegeId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        department.setName(request.getName());
        department.setCode(request.getCode());

        try {
            department = departmentRepository.save(department);
            return mapToDepartmentDto(department);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Department with this code already exists in your college");
        }
    }

    @Transactional
    public void deleteDepartment(UserPrincipal principal, Long id) {
        Department department = departmentRepository.findByIdAndCollegeId(id, principal.getCollegeId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        if (staffRepository.existsByDepartmentId(id) || studentRepository.existsByDepartmentId(id) || semesterRepository.existsByDepartmentId(id)) {
            throw new IllegalArgumentException("Cannot delete department because it contains staff, students, or semesters.");
        }

        departmentRepository.softDelete(id, principal.getCollegeId(), principal.getId());
    }

    // --- Academic Structure (Semester & Subject) ---

    public List<SemesterDto> getSemesters(UserPrincipal principal, Long departmentId) {
        // Validate department belongs to college
        departmentRepository.findByIdAndCollegeId(departmentId, principal.getCollegeId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        return semesterRepository.findAllByCollegeIdAndDepartmentId(principal.getCollegeId(), departmentId)
                .stream().map(this::mapToSemesterDto).collect(Collectors.toList());
    }

    @Transactional
    public SemesterDto createSemester(UserPrincipal principal, Long departmentId, CreateSemesterRequest request) {
        Department department = departmentRepository.findByIdAndCollegeId(departmentId, principal.getCollegeId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        
        College college = collegeRepository.findById(principal.getCollegeId())
                .orElseThrow(() -> new ResourceNotFoundException("College not found"));

        Semester semester = new Semester();
        semester.setDepartment(department);
        semester.setCollege(college);
        semester.setSemesterNumber(request.getSemesterNumber());

        try {
            semester = semesterRepository.save(semester);
            return mapToSemesterDto(semester);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Semester number already exists for this department");
        }
    }

    public List<SubjectDto> getSubjects(UserPrincipal principal, Long semesterId) {
        semesterRepository.findByIdAndCollegeId(semesterId, principal.getCollegeId())
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found"));

        return subjectRepository.findAllByCollegeIdAndSemesterId(principal.getCollegeId(), semesterId)
                .stream().map(this::mapToSubjectDto).collect(Collectors.toList());
    }

    @Transactional
    public SubjectDto createSubject(UserPrincipal principal, Long semesterId, CreateSubjectRequest request) {
        Semester semester = semesterRepository.findByIdAndCollegeId(semesterId, principal.getCollegeId())
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found"));
        
        College college = collegeRepository.findById(principal.getCollegeId())
                .orElseThrow(() -> new ResourceNotFoundException("College not found"));

        Subject subject = new Subject();
        subject.setSemester(semester);
        subject.setCollege(college);
        subject.setSubjectCode(request.getSubjectCode());
        subject.setSubjectName(request.getSubjectName());
        subject.setMaxMarks(request.getMaxMarks());

        try {
            subject = subjectRepository.save(subject);
            return mapToSubjectDto(subject);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Subject code already exists for this semester");
        }
    }

    @Transactional
    public SubjectDto updateSubject(UserPrincipal principal, Long id, UpdateSubjectRequest request) {
        Subject subject = subjectRepository.findByIdAndCollegeId(id, principal.getCollegeId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        subject.setSubjectCode(request.getSubjectCode());
        subject.setSubjectName(request.getSubjectName());
        subject.setMaxMarks(request.getMaxMarks());

        try {
            subject = subjectRepository.save(subject);
            return mapToSubjectDto(subject);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Subject code already exists for this semester");
        }
    }

    // --- Staff ---

    public PageResponse<StaffDto> getStaff(UserPrincipal principal, String search, Long departmentId, Pageable pageable) {
        Page<Staff> page = staffRepository.searchStaff(principal.getCollegeId(), departmentId, search, pageable);
        return new PageResponse<>(page.map(this::mapToStaffDto));
    }

    @Transactional
    public StaffDto createStaff(UserPrincipal principal, CreateStaffRequest request) {
        Department department = departmentRepository.findByIdAndCollegeId(request.getDepartmentId(), principal.getCollegeId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        
        College college = collegeRepository.findById(principal.getCollegeId())
                .orElseThrow(() -> new ResourceNotFoundException("College not found"));

        User user = new User();
        user.setEmail(request.getEmail().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.STAFF);
        user.setCollege(college);
        user.setIsActive(true);
        user.setIsVerified(true); // For MVP E2E testing, auto-verify

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Email already exists in this college");
        }

        Staff staff = new Staff();
        staff.setUser(user);
        staff.setDepartment(department);
        staff.setCollege(college);
        staff.setFullName(request.getFullName());
        staff.setDesignation(request.getDesignation());

        staff = staffRepository.save(staff);
        return mapToStaffDto(staff);
    }

    @Transactional
    public StaffDto updateStaffStatus(UserPrincipal principal, Long id, boolean isActive) {
        Staff staff = staffRepository.findByIdAndCollegeId(id, principal.getCollegeId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        staff.getUser().setIsActive(isActive);
        userRepository.save(staff.getUser());
        
        return mapToStaffDto(staff);
    }

    // --- Student ---

    public PageResponse<StudentDto> getStudents(UserPrincipal principal, String search, Long departmentId, Pageable pageable) {
        Page<Student> page = studentRepository.searchStudents(principal.getCollegeId(), departmentId, search, pageable);
        return new PageResponse<>(page.map(this::mapToStudentDto));
    }

    @Transactional
    public StudentDto createStudent(UserPrincipal principal, CreateStudentRequest request) {
        Department department = departmentRepository.findByIdAndCollegeId(request.getDepartmentId(), principal.getCollegeId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        
        College college = collegeRepository.findById(principal.getCollegeId())
                .orElseThrow(() -> new ResourceNotFoundException("College not found"));

        User user = new User();
        user.setEmail(request.getEmail().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.STUDENT);
        user.setCollege(college);
        user.setIsActive(true);
        user.setIsVerified(true); // For MVP E2E testing, auto-verify

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Email already exists in this college");
        }

        Student student = new Student();
        student.setUser(user);
        student.setDepartment(department);
        student.setCollege(college);
        student.setFullName(request.getFullName());
        student.setRollNumber(request.getRollNumber());
        student.setUniversityRegisterNo(request.getUniversityRegisterNo());
        student.setDob(request.getDob());
        student.setBatchYear(request.getBatchYear());
        student.setCurrentSemester(1);

        try {
            student = studentRepository.save(student);
            return mapToStudentDto(student);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Roll number or Register number already exists");
        }
    }

    @Transactional
    public StudentDto updateStudentStatus(UserPrincipal principal, Long id, boolean isActive) {
        Student student = studentRepository.findByIdAndCollegeId(id, principal.getCollegeId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        student.getUser().setIsActive(isActive);
        userRepository.save(student.getUser());
        
        return mapToStudentDto(student);
    }

    // --- Mapping Helpers ---

    private DepartmentDto mapToDepartmentDto(Department d) {
        DepartmentDto dto = new DepartmentDto();
        dto.setId(d.getId());
        dto.setName(d.getName());
        dto.setCode(d.getCode());
        return dto;
    }

    private SemesterDto mapToSemesterDto(Semester s) {
        SemesterDto dto = new SemesterDto();
        dto.setId(s.getId());
        dto.setDepartmentId(s.getDepartment().getId());
        dto.setSemesterNumber(s.getSemesterNumber());
        return dto;
    }

    private SubjectDto mapToSubjectDto(Subject s) {
        SubjectDto dto = new SubjectDto();
        dto.setId(s.getId());
        dto.setSemesterId(s.getSemester().getId());
        dto.setSubjectCode(s.getSubjectCode());
        dto.setSubjectName(s.getSubjectName());
        dto.setMaxMarks(s.getMaxMarks());
        return dto;
    }

    private StaffDto mapToStaffDto(Staff s) {
        StaffDto dto = new StaffDto();
        dto.setId(s.getId());
        dto.setDepartmentId(s.getDepartment().getId());
        dto.setDepartmentName(s.getDepartment().getName());
        dto.setEmail(s.getUser().getEmail());
        dto.setFullName(s.getFullName());
        dto.setDesignation(s.getDesignation());
        dto.setActive(s.getUser().getIsActive());
        dto.setVerified(s.getUser().getIsVerified());
        dto.setCreatedAt(s.getCreatedAt());
        return dto;
    }

    private StudentDto mapToStudentDto(Student s) {
        StudentDto dto = new StudentDto();
        dto.setId(s.getId());
        dto.setDepartmentId(s.getDepartment().getId());
        dto.setDepartmentName(s.getDepartment().getName());
        dto.setEmail(s.getUser().getEmail());
        dto.setFullName(s.getFullName());
        dto.setRollNumber(s.getRollNumber());
        dto.setUniversityRegisterNo(s.getUniversityRegisterNo());
        dto.setDob(s.getDob());
        dto.setBatchYear(s.getBatchYear());
        dto.setCurrentSemester(s.getCurrentSemester());
        dto.setActive(s.getUser().getIsActive());
        dto.setVerified(s.getUser().getIsVerified());
        dto.setCreatedAt(s.getCreatedAt());
        return dto;
    }
}
