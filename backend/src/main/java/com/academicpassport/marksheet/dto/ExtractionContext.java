package com.academicpassport.marksheet.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExtractionContext {
    private String fileKey;
    private String expectedStudentName;
    private String expectedRegisterNumber;
    private Integer expectedSemesterNumber;
    private String expectedInstitutionName;
}
