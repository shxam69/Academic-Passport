package com.academicpassport.college.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class CollegeInvitationDto {
    private String institutionName;
    private String adminEmail;
    private Instant expiresAt;
    private String status;
    private Long id;
    private String token; // Only populated on creation
}
