package com.academicpassport.universityadmin.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class StaffDto {
    private Long id;
    private Long departmentId;
    private String departmentName;
    private String email;
    private String fullName;
    private String designation;
    private boolean isActive;
    private boolean isVerified;
    private Instant createdAt;
}
