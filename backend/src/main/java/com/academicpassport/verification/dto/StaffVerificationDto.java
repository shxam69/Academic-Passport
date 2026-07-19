package com.academicpassport.verification.dto;

import com.academicpassport.verification.VerificationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class StaffVerificationDto {
    private Long marksheetId;
    private String studentName;
    private String registerNumber;
    private Integer semesterNumber;
    private String departmentName;
    private VerificationStatus status;
    private Instant uploadedAt;
    private Instant verifiedAt;
    
    // OCR fields
    private String ocrStatus;
    private Double confidenceScore;
    private List<String> findings;
    
    private List<StaffSubjectDto> subjects;
    
    @Data
    @Builder
    public static class StaffSubjectDto {
        private Long subjectId;
        private String subjectCode;
        private String subjectName;
        private Integer maxMarks;
        private Integer aiExtractedMarks;
        private Integer correctedMarks;
        private Integer verifiedMarks;
        private String grade;
    }
}
