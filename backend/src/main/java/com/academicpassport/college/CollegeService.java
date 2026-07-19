package com.academicpassport.college;

import com.academicpassport.auth.User;
import com.academicpassport.auth.UserRepository;
import com.academicpassport.auth.UserRole;
import com.academicpassport.college.dto.CollegeDetailsDto;
import com.academicpassport.college.dto.CollegeDto;
import com.academicpassport.college.dto.CreateCollegeRequest;
import com.academicpassport.college.dto.CreateUniversityAdminRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CollegeService {

    private final CollegeRepository collegeRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public CollegeService(CollegeRepository collegeRepository, UserRepository userRepository, JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.collegeRepository = collegeRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    public Page<CollegeDto> searchColleges(Boolean isActive, String search, Pageable pageable) {
        String searchParam = (search != null && !search.trim().isEmpty()) ? search.trim() : "";
        Page<College> colleges = collegeRepository.searchColleges(isActive, searchParam, pageable);
        return colleges.map(CollegeDto::fromEntity);
    }

    @Transactional
    public CollegeDto createCollege(CreateCollegeRequest request) {
        Long seq = jdbcTemplate.queryForObject("SELECT nextval('college_code_seq')", Long.class);
        String collegeCode = String.format("AP-%06d", seq);

        College college = new College();
        college.setName(request.getName());
        college.setAddress(request.getAddress());
        college.setCollegeCode(collegeCode);
        college.setIsActive(true);

        College savedCollege = collegeRepository.save(college);
        return CollegeDto.fromEntity(savedCollege);
    }

    public CollegeDetailsDto getCollegeDetails(Long id) {
        College college = collegeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("College not found"));
        
        List<User> admins = userRepository.findByCollegeIdAndRole(id, UserRole.UNIVERSITY_ADMIN);
        
        return CollegeDetailsDto.fromEntityWithAdmins(college, admins);
    }

    @Transactional
    public void updateCollegeStatus(Long id, boolean isActive) {
        College college = collegeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("College not found"));
        college.setIsActive(isActive);
        collegeRepository.save(college);
    }

    @Transactional
    public void createUniversityAdmin(Long collegeId, CreateUniversityAdminRequest request) {
        College college = collegeRepository.findById(collegeId)
                .orElseThrow(() -> new IllegalArgumentException("College not found"));

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        
        // We must check if email is unique within the college, or globally if needed.
        // The DB constraint is uq_users_college_email for non-super-admins.
        if (userRepository.findByCollegeIdAndEmail(collegeId, normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("Email is already registered for this college");
        }

        User admin = new User();
        admin.setCollege(college);
        admin.setEmail(normalizedEmail);
        admin.setMobile(request.getMobile());
        admin.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        admin.setRole(UserRole.UNIVERSITY_ADMIN);
        admin.setIsActive(true);
        admin.setIsVerified(true); // Super Admin provisioned, auto-verify

        userRepository.save(admin);
    }
}
