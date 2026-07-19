package com.academicpassport.college.dto;

import com.academicpassport.college.College;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class CollegeDto {
    private Long id;
    private String name;
    private String collegeCode;
    private String address;
    private Boolean isActive;
    private Instant createdAt;
    
    public static CollegeDto fromEntity(College college) {
        CollegeDto dto = new CollegeDto();
        dto.setId(college.getId());
        dto.setName(college.getName());
        dto.setCollegeCode(college.getCollegeCode());
        dto.setAddress(college.getAddress());
        dto.setIsActive(college.getIsActive());
        dto.setCreatedAt(college.getCreatedAt());
        return dto;
    }
}
