package com.academicpassport.verification;

import com.academicpassport.auth.UserPrincipal;
import com.academicpassport.staff.Staff;
import com.academicpassport.staff.StaffRepository;
import com.academicpassport.storage.FileStorageService;
import com.academicpassport.verification.dto.StaffVerificationDto;
import com.academicpassport.verification.service.StaffVerificationService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/staff/verifications")
public class StaffVerificationController {

    private final StaffVerificationService verificationService;
    private final StaffRepository staffRepository;
    private final FileStorageService fileStorageService;

    public StaffVerificationController(StaffVerificationService verificationService,
                                       StaffRepository staffRepository,
                                       FileStorageService fileStorageService) {
        this.verificationService = verificationService;
        this.staffRepository = staffRepository;
        this.fileStorageService = fileStorageService;
    }

    private Staff getCurrentStaff(UserPrincipal principal) {
        return staffRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new IllegalStateException("Staff profile not found"));
    }

    @GetMapping("/marksheets")
    public List<StaffVerificationDto> getPendingQueue(@AuthenticationPrincipal UserPrincipal principal) {
        return verificationService.getPendingQueue(getCurrentStaff(principal));
    }

    @GetMapping("/marksheets/{id}")
    public StaffVerificationDto getVerification(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return verificationService.getVerificationDto(getCurrentStaff(principal), id);
    }

    @GetMapping("/marksheets/{id}/file")
    public ResponseEntity<Resource> getMarksheetFile(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        Staff staff = getCurrentStaff(principal);
        Verification verification = verificationService.getVerification(staff, id);
        
        Resource resource = fileStorageService.loadFileAsResource(verification.getMarksheet().getFileKey());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF) // Defaulting to PDF, ideally determined by file extension/MIME
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"marksheet_" + id + "\"")
                .body(resource);
    }

    @PutMapping("/marksheets/{id}/subjects/{subjectId}")
    public ResponseEntity<Void> updateSubjectMarks(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable Long id, 
                                                   @PathVariable Long subjectId,
                                                   @RequestBody SubjectUpdateDto dto) {
        verificationService.updateSubjectMarks(getCurrentStaff(principal), id, subjectId, dto.getMarks());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/marksheets/{id}/approve")
    public ResponseEntity<Void> approveMarksheet(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        verificationService.approveMarksheet(getCurrentStaff(principal), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/marksheets/{id}/reject")
    public ResponseEntity<Void> rejectMarksheet(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id, @RequestBody RejectDto dto) {
        verificationService.rejectMarksheet(getCurrentStaff(principal), id, dto.getReason());
        return ResponseEntity.noContent().build();
    }
    
    public static class SubjectUpdateDto {
        private Integer marks;
        public Integer getMarks() { return marks; }
        public void setMarks(Integer marks) { this.marks = marks; }
    }
    
    public static class RejectDto {
        private String reason;
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
