package com.academicpassport.marksheet.dto;

import lombok.Data;
import java.util.List;

@Data
public class ExtractionResult {
    private String studentName;
    private String registerNumber;
    private Integer semesterNumber;
    private String institutionName;
    private List<ExtractedSubject> subjects;

    @Data
    public static class ExtractedSubject {
        private String subjectCode;
        private String subjectName;
        // Integer object used to preserve null if value is absent/unreadable
        private Integer marksObtained; 
        private String grade;
        private Integer totalMarks; // Used for validation context, not strictly saved
    }
}
