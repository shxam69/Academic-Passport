package com.academicpassport.college.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCollegeRequest {
    @NotBlank(message = "College name is required")
    private String name;
    
    private String address;
}
