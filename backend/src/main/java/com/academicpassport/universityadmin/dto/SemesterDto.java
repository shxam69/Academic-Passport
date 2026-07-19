package com.academicpassport.universityadmin.dto;

import lombok.Data;

@Data
public class SemesterDto {
    private Long id;
    private Long departmentId;
    private Integer semesterNumber;
}
