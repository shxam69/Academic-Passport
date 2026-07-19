package com.academicpassport.college;

import com.academicpassport.auth.TokenGeneratorUtil;
import com.academicpassport.auth.TokenHashUtil;
import com.academicpassport.auth.User;
import com.academicpassport.auth.UserRepository;
import com.academicpassport.auth.UserRole;
import com.academicpassport.college.dto.CollegeInvitationDto;
import com.academicpassport.college.dto.CreateInvitationRequest;
import com.academicpassport.college.dto.OnboardingRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class InstitutionalOnboardingService {

    private final CollegeInvitationRepository invitationRepository;
    private final CollegeRepository collegeRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public InstitutionalOnboardingService(CollegeInvitationRepository invitationRepository,
                                          CollegeRepository collegeRepository,
                                          UserRepository userRepository,
                                          JdbcTemplate jdbcTemplate,
                                          PasswordEncoder passwordEncoder) {
        this.invitationRepository = invitationRepository;
        this.collegeRepository = collegeRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public String generateInvitation(CreateInvitationRequest request, User createdBy) {
        // Clean up inputs
        String normalizedEmail = request.getAdminEmail().trim().toLowerCase();
        
        // Ensure no existing PENDING invites for this email/institution pair block this intentionally
        // We will revoke existing ones for this email to prevent spam/duplicate logic
        List<CollegeInvitation> existing = invitationRepository.findByAdminEmailAndStatus(normalizedEmail, CollegeInvitationStatus.PENDING);
        for (CollegeInvitation inv : existing) {
            inv.setStatus(CollegeInvitationStatus.REVOKED);
            invitationRepository.save(inv);
        }
        
        // Check if a user with this email already exists
        if (!userRepository.findByEmail(normalizedEmail).isEmpty()) {
            throw new IllegalArgumentException("Email is already registered in the system.");
        }

        // Generate secure token
        String rawToken = TokenGeneratorUtil.generateSecureToken();
        String tokenHash = TokenHashUtil.hashToken(rawToken);

        CollegeInvitation invitation = new CollegeInvitation();
        invitation.setTokenHash(tokenHash);
        invitation.setInstitutionName(request.getInstitutionName().trim());
        invitation.setAdminEmail(normalizedEmail);
        invitation.setStatus(CollegeInvitationStatus.PENDING);
        invitation.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        invitation.setCreatedBy(createdBy);

        invitationRepository.save(invitation);

        return rawToken;
    }

    @Transactional(readOnly = true)
    public Page<CollegeInvitationDto> getInvitations(CollegeInvitationStatus status, Pageable pageable) {
        return invitationRepository.findByStatus(status, pageable)
            .map(inv -> {
                CollegeInvitationDto dto = new CollegeInvitationDto();
                dto.setId(inv.getId());
                dto.setInstitutionName(inv.getInstitutionName());
                dto.setAdminEmail(inv.getAdminEmail());
                dto.setExpiresAt(inv.getExpiresAt());
                // Dynamically report EXPIRED if past time, otherwise DB status
                if (inv.getStatus() == CollegeInvitationStatus.PENDING && inv.isExpired()) {
                    dto.setStatus("EXPIRED");
                } else {
                    dto.setStatus(inv.getStatus().name());
                }
                return dto;
            });
    }

    @Transactional(readOnly = true)
    public CollegeInvitationDto validateInvitation(String rawToken) {
        String tokenHash = TokenHashUtil.hashToken(rawToken);
        CollegeInvitation invitation = invitationRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or non-existent invitation token"));

        if (invitation.getStatus() == CollegeInvitationStatus.USED) {
            throw new IllegalArgumentException("This invitation has already been used");
        }
        if (invitation.getStatus() == CollegeInvitationStatus.REVOKED) {
            throw new IllegalArgumentException("This invitation has been revoked");
        }
        if (invitation.isExpired()) {
            throw new IllegalArgumentException("This invitation has expired");
        }

        CollegeInvitationDto dto = new CollegeInvitationDto();
        dto.setInstitutionName(invitation.getInstitutionName());
        dto.setAdminEmail(invitation.getAdminEmail());
        dto.setExpiresAt(invitation.getExpiresAt());
        return dto;
    }

    @Transactional
    public void finalizeOnboarding(OnboardingRequest request, String rawToken) {
        // Validation of token and atomic consumption
        String tokenHash = TokenHashUtil.hashToken(rawToken);
        
        // We use an atomic update to guarantee concurrency protection.
        // If two threads try to consume simultaneously, only one update will succeed.
        int updatedCount = jdbcTemplate.update(
            "UPDATE college_invitations SET status = 'USED', updated_at = now() WHERE token_hash = ? AND status = 'PENDING' AND expires_at > now()",
            tokenHash
        );
        
        if (updatedCount == 0) {
            throw new IllegalArgumentException("Invalid, expired, or already used invitation");
        }

        CollegeInvitation invitation = invitationRepository.findByTokenHash(tokenHash).get();

        String normalizedEmail = invitation.getAdminEmail().toLowerCase();

        // Check again if email was taken in the meantime
        if (!userRepository.findByEmail(normalizedEmail).isEmpty()) {
            throw new IllegalArgumentException("Email is already registered in the system.");
        }

        // 1. Create College
        Long seq = jdbcTemplate.queryForObject("SELECT nextval('college_code_seq')", Long.class);
        String collegeCode = String.format("AP-%06d", seq);

        College college = new College();
        college.setName(request.getInstitutionName().trim());
        college.setCollegeCode(collegeCode);
        college.setAddressLine(request.getAddressLine());
        college.setCity(request.getCity());
        college.setState(request.getState());
        college.setPostalCode(request.getPostalCode());
        college.setCountry(request.getCountry());
        college.setContactName(request.getContactName());
        college.setContactEmail(normalizedEmail); // Must match the invitation
        college.setContactPhone(request.getContactPhone());
        college.setInstitutionType(request.getInstitutionType());
        college.setWebsite(request.getWebsite());
        // For MVP, immediately ACTIVE
        college.setIsActive(true);

        college = collegeRepository.save(college);

        // 2. Create University Admin
        User admin = new User();
        admin.setCollege(college);
        admin.setEmail(normalizedEmail);
        admin.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        admin.setRole(UserRole.UNIVERSITY_ADMIN);
        admin.setIsActive(true);
        admin.setIsVerified(true); 

        userRepository.save(admin);
    }

    @Transactional
    public void revokeInvitation(Long id) {
        CollegeInvitation invitation = invitationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));
        
        if (invitation.getStatus() == CollegeInvitationStatus.USED) {
            throw new IllegalArgumentException("Cannot revoke a used invitation");
        }
        
        invitation.setStatus(CollegeInvitationStatus.REVOKED);
        invitationRepository.save(invitation);
    }
}
