package com.academicpassport.marksheet.service;

import com.academicpassport.college.Subject;
import com.academicpassport.marksheet.ExtractionFinding;
import com.academicpassport.marksheet.dto.ExtractionResult;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class OcrValidationResult {
    private boolean reviewRequired = false;
    private BigDecimal confidenceScore = BigDecimal.valueOf(100.0);
    private List<ExtractionFinding> findings = new ArrayList<>();
    private List<MatchedSubject> matchedSubjects = new ArrayList<>();

    @Data
    public static class MatchedSubject {
        private Subject expectedSubject;
        private ExtractionResult.ExtractedSubject extracted;
        private boolean exactMatch;
    }
    
    public void addFinding(ExtractionFinding finding) {
        if (!findings.contains(finding)) {
            findings.add(finding);
        }
        reviewRequired = true;
    }
}
