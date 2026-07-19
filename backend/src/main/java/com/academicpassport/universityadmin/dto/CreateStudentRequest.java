package com.academicpassport.universityadmin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateStudentRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Roll number is required")
    private String rollNumber;

    @NotBlank(message = "University register number is required")
    private String universityRegisterNo;

    @NotNull(message = "Date of birth is required")
    private LocalDate dob;

    @NotNull(message = "Batch year is required")
    private Integer batchYear;

    @NotNull(message = "Department ID is required")
    private Long departmentId;
}
