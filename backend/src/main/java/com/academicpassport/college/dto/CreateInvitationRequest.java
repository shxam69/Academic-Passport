package com.academicpassport.college.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateInvitationRequest {
    @NotBlank(message = "Institution name is required")
    private String institutionName;
    
    @NotBlank(message = "Admin email is required")
    @Email(message = "Admin email must be valid")
    private String adminEmail;
}
