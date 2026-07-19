package com.academicpassport.college.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUniversityAdminRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,}$", 
             message = "Password must be at least 8 characters long and contain at least one letter and one number")
    private String password;
    
    private String mobile;
}
