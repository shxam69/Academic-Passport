package com.academicpassport.marksheet.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class MarksheetDto {
    private Long id;
    private Long semesterId;
    private String semesterName;
    private String status; // Derived from OCR/Verification state
    private Instant uploadedAt;
}
