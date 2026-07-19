package com.academicpassport.verification.service;

import com.academicpassport.college.Subject;
import com.academicpassport.marksheet.Marksheet;
import com.academicpassport.marksheet.MarksheetRepository;
import com.academicpassport.marksheet.MarksheetSubject;
import com.academicpassport.marksheet.MarksheetSubjectRepository;
import com.academicpassport.marksheet.OcrResult;
import com.academicpassport.marksheet.OcrResultRepository;
import com.academicpassport.staff.Staff;
import com.academicpassport.verification.Verification;
import com.academicpassport.verification.VerificationRepository;
import com.academicpassport.verification.VerificationStatus;
import com.academicpassport.verification.dto.StaffVerificationDto;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StaffVerificationService {

    private final VerificationRepository verificationRepository;
    private final MarksheetRepository marksheetRepository;
    private final MarksheetSubjectRepository marksheetSubjectRepository;
    private final OcrResultRepository ocrResultRepository;
    private final ObjectMapper objectMapper;

    public StaffVerificationService(VerificationRepository verificationRepository,
                                    MarksheetRepository marksheetRepository,
                                    MarksheetSubjectRepository marksheetSubjectRepository,
                                    OcrResultRepository ocrResultRepository,
                                    ObjectMapper objectMapper) {
        this.verificationRepository = verificationRepository;
        this.marksheetRepository = marksheetRepository;
        this.marksheetSubjectRepository = marksheetSubjectRepository;
        this.ocrResultRepository = ocrResultRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<StaffVerificationDto> getPendingQueue(Staff staff) {
        List<Verification> verifications = verificationRepository.findPendingQueue(staff.getCollege().getId(), staff.getDepartment().getId());
        return verifications.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Verification getVerification(Staff staff, Long marksheetId) {
        Verification verification = verificationRepository.findByMarksheetId(marksheetId)
                .orElseThrow(() -> new IllegalArgumentException("Verification not found"));
        
        if (!verification.getMarksheet().getCollege().getId().equals(staff.getCollege().getId())) {
            throw new SecurityException("Cannot access verification outside of your college");
        }
        
        // MVP: Optional department scoping, currently staff is scoped to department
        if (!verification.getMarksheet().getStudent().getDepartment().getId().equals(staff.getDepartment().getId())) {
            throw new SecurityException("Cannot access verification outside of your department");
        }
        
        return verification;
    }

    @Transactional
    public void updateSubjectMarks(Staff staff, Long marksheetId, Long subjectId, Integer newMarks) {
        Verification verification = getVerification(staff, marksheetId);
        if (verification.getStatus() == VerificationStatus.APPROVED) {
            throw new IllegalStateException("Cannot edit an approved marksheet");
        }
        
        MarksheetSubject ms = marksheetSubjectRepository.findByMarksheetIdAndSubjectId(marksheetId, subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found for marksheet"));
        
        if (newMarks != null && newMarks > ms.getSubject().getMaxMarks()) {
            throw new IllegalArgumentException("Marks cannot exceed maximum marks for subject");
        }
        
        ms.setCorrectedMarks(newMarks);
        ms.setIsEditedByStaff(true);
        marksheetSubjectRepository.save(ms);
    }

    @Transactional
    public void approveMarksheet(Staff staff, Long marksheetId) {
        Verification verification = getVerification(staff, marksheetId);
        
        if (verification.getStatus() == VerificationStatus.APPROVED) {
            throw new IllegalStateException("Marksheet is already approved");
        }
        
        List<MarksheetSubject> subjects = marksheetSubjectRepository.findAllByMarksheetId(marksheetId);
        for (MarksheetSubject ms : subjects) {
            if (ms.getCorrectedMarks() == null) {
                throw new IllegalStateException("Cannot approve with missing marks for subject: " + ms.getSubject().getSubjectCode());
            }
            if (ms.getCorrectedMarks() > ms.getSubject().getMaxMarks()) {
                throw new IllegalStateException("Invalid marks for subject: " + ms.getSubject().getSubjectCode());
            }
            ms.setVerifiedMarks(ms.getCorrectedMarks());
            marksheetSubjectRepository.save(ms);
        }
        
        verification.setStaff(staff);
        verification.setStatus(VerificationStatus.APPROVED);
        verification.setVerifiedAt(Instant.now());
        verificationRepository.save(verification);
    }

    @Transactional
    public void rejectMarksheet(Staff staff, Long marksheetId, String reason) {
        Verification verification = getVerification(staff, marksheetId);
        
        if (verification.getStatus() == VerificationStatus.APPROVED) {
            throw new IllegalStateException("Cannot reject an already approved marksheet");
        }
        
        verification.setStaff(staff);
        verification.setStatus(VerificationStatus.REJECTED);
        verification.setRejectionReason(reason);
        verification.setVerifiedAt(Instant.now());
        verificationRepository.save(verification);
    }

    @Transactional(readOnly = true)
    public StaffVerificationDto getVerificationDto(Staff staff, Long marksheetId) {
        Verification verification = getVerification(staff, marksheetId);
        return mapToDto(verification);
    }

    private StaffVerificationDto mapToDto(Verification v) {
        Marksheet m = v.getMarksheet();
        OcrResult ocr = ocrResultRepository.findByMarksheetId(m.getId()).orElse(null);
        List<MarksheetSubject> subjects = marksheetSubjectRepository.findAllByMarksheetId(m.getId());
        
        List<String> findingsList = new ArrayList<>();
        if (ocr != null && ocr.getFindings() != null) {
            try {
                findingsList = objectMapper.readValue(ocr.getFindings(), new TypeReference<List<String>>() {});
            } catch (Exception e) {
                // ignore
            }
        }
        
        List<StaffVerificationDto.StaffSubjectDto> subjectDtos = subjects.stream().map(ms -> StaffVerificationDto.StaffSubjectDto.builder()
                .subjectId(ms.getSubject().getId())
                .subjectCode(ms.getSubject().getSubjectCode())
                .subjectName(ms.getSubject().getSubjectName())
                .maxMarks(ms.getSubject().getMaxMarks())
                .aiExtractedMarks(ms.getAiExtractedMarks())
                .correctedMarks(ms.getCorrectedMarks())
                .verifiedMarks(ms.getVerifiedMarks())
                .grade(ms.getGrade())
                .build()).collect(Collectors.toList());
                
        return StaffVerificationDto.builder()
                .marksheetId(m.getId())
                .studentName(m.getStudent().getFullName())
                .registerNumber(m.getStudent().getUniversityRegisterNo())
                .semesterNumber(m.getSemester().getSemesterNumber())
                .departmentName(m.getStudent().getDepartment().getName())
                .status(v.getStatus())
                .uploadedAt(m.getUploadedAt())
                .verifiedAt(v.getVerifiedAt())
                .ocrStatus(ocr != null ? ocr.getStatus().name() : null)
                .confidenceScore(ocr != null && ocr.getConfidenceScore() != null ? ocr.getConfidenceScore().doubleValue() : null)
                .findings(findingsList)
                .subjects(subjectDtos)
                .build();
    }
}
