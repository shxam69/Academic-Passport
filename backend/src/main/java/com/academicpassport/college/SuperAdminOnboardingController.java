package com.academicpassport.college;

import com.academicpassport.auth.User;
import com.academicpassport.college.dto.CollegeInvitationDto;
import com.academicpassport.college.dto.CreateInvitationRequest;
import com.academicpassport.college.dto.OnboardingRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/invitations")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminOnboardingController {

    private final InstitutionalOnboardingService onboardingService;

    public SuperAdminOnboardingController(InstitutionalOnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> generateInvitation(
            @Valid @RequestBody CreateInvitationRequest request,
            @AuthenticationPrincipal User createdBy) {
        String token = onboardingService.generateInvitation(request, createdBy);
        // For MVP, we return the token in the response so the UI can construct the link
        return ResponseEntity.ok(Map.of("token", token));
    }

    @GetMapping
    public Page<CollegeInvitationDto> getInvitations(
            @RequestParam(required = false) CollegeInvitationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return onboardingService.getInvitations(status, pageable);
    }

    @PostMapping("/{id}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeInvitation(@PathVariable Long id) {
        onboardingService.revokeInvitation(id);
    }
    
    @PostMapping("/onboard")
    @ResponseStatus(HttpStatus.CREATED)
    public void manualOnboard(@Valid @RequestBody OnboardingRequest request, @AuthenticationPrincipal User createdBy) {
        // We reuse the invitation flow internally to maintain exactly ONE canonical convergence point!
        // The service could take the request directly, but creating a dummy token 
        // guarantees it goes through the EXACT same atomic logic.
        // Wait, InstitutionalOnboardingService requires a token hash to mark it USED.
        // Actually, it's better to add a direct method in the service or just generate an instant token.
        
        CreateInvitationRequest inviteReq = new CreateInvitationRequest();
        inviteReq.setInstitutionName(request.getInstitutionName());
        inviteReq.setAdminEmail(request.getContactEmail());
        String token = onboardingService.generateInvitation(inviteReq, createdBy);
        
        onboardingService.finalizeOnboarding(request, token);
    }
}
