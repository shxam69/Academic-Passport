package com.academicpassport.marksheet.dto;

import lombok.Data;

@Data
public class OcrProcessingStatusDto {
    private String status;
    private String displayMessage;
    private Boolean validationPassed;
}
