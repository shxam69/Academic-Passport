package com.academicpassport.universityadmin.dto;

import lombok.Data;
import java.time.Instant;
import java.time.LocalDate;

@Data
public class StudentDto {
    private Long id;
    private Long departmentId;
    private String departmentName;
    private String email;
    private String fullName;
    private String rollNumber;
    private String universityRegisterNo;
    private LocalDate dob;
    private Integer batchYear;
    private Integer currentSemester;
    private boolean isActive;
    private boolean isVerified;
    private Instant createdAt;
}
