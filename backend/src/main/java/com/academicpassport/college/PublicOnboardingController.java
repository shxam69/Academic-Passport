package com.academicpassport.college;

import com.academicpassport.college.dto.CollegeInvitationDto;
import com.academicpassport.college.dto.OnboardingRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/onboarding")
public class PublicOnboardingController {

    private final InstitutionalOnboardingService onboardingService;

    public PublicOnboardingController(InstitutionalOnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @GetMapping("/invitations/{token}")
    public ResponseEntity<CollegeInvitationDto> validateInvitation(@PathVariable String token) {
        return ResponseEntity.ok(onboardingService.validateInvitation(token));
    }

    @PostMapping("/submit")
    public ResponseEntity<Void> submitOnboarding(
            @RequestParam("token") String token,
            @Valid @RequestBody OnboardingRequest request) {
        onboardingService.finalizeOnboarding(request, token);
        return ResponseEntity.ok().build();
    }
}
