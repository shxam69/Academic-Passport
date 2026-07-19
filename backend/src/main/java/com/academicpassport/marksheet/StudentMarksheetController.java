package com.academicpassport.marksheet;

import com.academicpassport.auth.UserPrincipal;
import com.academicpassport.college.Semester;
import com.academicpassport.marksheet.dto.MarksheetDto;
import com.academicpassport.marksheet.dto.OcrProcessingStatusDto;
import com.academicpassport.marksheet.service.StudentMarksheetService;
import com.academicpassport.verification.Verification;
import com.academicpassport.verification.VerificationRepository;
import com.academicpassport.verification.VerificationStatus;
import com.academicpassport.student.Student;
import com.academicpassport.student.StudentRepository;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/student/marksheets")
public class StudentMarksheetController {

    private final StudentMarksheetService marksheetService;
    private final StudentRepository studentRepository;
    private final OcrResultRepository ocrResultRepository;
    private final VerificationRepository verificationRepository;

    public StudentMarksheetController(StudentMarksheetService marksheetService, 
                                      StudentRepository studentRepository,
                                      OcrResultRepository ocrResultRepository,
                                      VerificationRepository verificationRepository) {
        this.marksheetService = marksheetService;
        this.studentRepository = studentRepository;
        this.ocrResultRepository = ocrResultRepository;
        this.verificationRepository = verificationRepository;
    }

    private Student getAuthenticatedStudent(UserPrincipal principal) {
        return studentRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Student profile not found"));
    }

    @GetMapping("/eligible-semesters")
    public ResponseEntity<List<Semester>> getEligibleSemesters(@AuthenticationPrincipal UserPrincipal principal) {
        Student student = getAuthenticatedStudent(principal);
        List<Semester> semesters = marksheetService.getEligibleSemesters(student);
        return ResponseEntity.ok(semesters);
    }

    @PostMapping
    public ResponseEntity<MarksheetDto> uploadMarksheet(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("semesterId") Long semesterId,
            @RequestParam("file") MultipartFile file) {
        try {
            Student student = getAuthenticatedStudent(principal);
            Marksheet marksheet = marksheetService.uploadMarksheet(student, semesterId, file);
            return ResponseEntity.status(HttpStatus.CREATED).body(toDto(marksheet));
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PutMapping("/{marksheetId}/file")
    public ResponseEntity<MarksheetDto> replaceMarksheet(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long marksheetId,
            @RequestParam("file") MultipartFile file) {
        try {
            Student student = getAuthenticatedStudent(principal);
            Marksheet marksheet = marksheetService.replaceMarksheet(student, marksheetId, file);
            return ResponseEntity.ok(toDto(marksheet));
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<MarksheetDto>> getMyMarksheets(@AuthenticationPrincipal UserPrincipal principal) {
        Student student = getAuthenticatedStudent(principal);
        List<MarksheetDto> marksheets = marksheetService.getStudentMarksheets(student).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(marksheets);
    }

    @GetMapping("/{marksheetId}/file")
    public ResponseEntity<Resource> downloadMarksheet(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long marksheetId) {
        try {
            Student student = getAuthenticatedStudent(principal);
            Resource resource = marksheetService.downloadMarksheet(student, marksheetId);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"marksheet_" + marksheetId + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Marksheet not found");
        }
    }

    @GetMapping("/{marksheetId}/processing-status")
    public ResponseEntity<OcrProcessingStatusDto> getProcessingStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long marksheetId) {
        Student student = getAuthenticatedStudent(principal);
        // marksheetService will verify ownership implicitly via getting it or we can just find it
        Marksheet marksheet = marksheetService.getStudentMarksheets(student).stream()
                .filter(m -> m.getId().equals(marksheetId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Marksheet not found"));

        OcrResult ocrResult = ocrResultRepository.findByMarksheetId(marksheet.getId()).orElse(null);
        Verification verification = verificationRepository.findByMarksheetId(marksheet.getId()).orElse(null);
        
        OcrProcessingStatusDto dto = new OcrProcessingStatusDto();
        dto.setStatus(determineState(ocrResult, verification));
        dto.setDisplayMessage(determineDisplayMessage(dto.getStatus()));
        dto.setValidationPassed(ocrResult != null ? ocrResult.getValidationPassed() : null);
        
        return ResponseEntity.ok(dto);
    }

    private MarksheetDto toDto(Marksheet marksheet) {
        MarksheetDto dto = new MarksheetDto();
        dto.setId(marksheet.getId());
        dto.setSemesterId(marksheet.getSemester().getId());
        dto.setSemesterName("Semester " + marksheet.getSemester().getSemesterNumber());
        dto.setUploadedAt(marksheet.getUploadedAt());
        
        OcrResult ocrResult = ocrResultRepository.findByMarksheetId(marksheet.getId()).orElse(null);
        Verification verification = verificationRepository.findByMarksheetId(marksheet.getId()).orElse(null);
        
        String state = determineState(ocrResult, verification);
        dto.setStatus(determineDisplayMessage(state));
        
        return dto;
    }

    private String determineState(OcrResult ocrResult, Verification verification) {
        if (ocrResult == null || ocrResult.getStatus() == OcrStatus.PENDING || ocrResult.getStatus() == OcrStatus.PROCESSING) {
            return "PROCESSING";
        }
        if (ocrResult.getStatus() == OcrStatus.FAILED || ocrResult.getStatus() == OcrStatus.FAILED_RETRYABLE) {
            return "PROCESSING_FAILED";
        }
        if (verification != null && verification.getStatus() == VerificationStatus.APPROVED) {
            return "VERIFIED";
        }
        if (ocrResult.getStatus() == OcrStatus.COMPLETED) {
            return Boolean.TRUE.equals(ocrResult.getValidationPassed()) ? "READY_FOR_REVIEW" : "REVIEW_REQUIRED";
        }
        return "UNKNOWN";
    }

    private String determineDisplayMessage(String state) {
        return switch (state) {
            case "AWAITING_PROCESSING" -> "Uploaded — awaiting processing";
            case "PROCESSING" -> "Processing...";
            case "PROCESSING_FAILED" -> "Processing failed";
            case "READY_FOR_REVIEW" -> "Processing completed";
            case "REVIEW_REQUIRED" -> "Review required";
            case "VERIFIED" -> "Verified";
            default -> "Unknown";
        };
    }
}
