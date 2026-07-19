package com.academicpassport.universityadmin.dto;

import lombok.Data;

@Data
public class SubjectDto {
    private Long id;
    private Long semesterId;
    private String subjectCode;
    private String subjectName;
    private Integer maxMarks;
}
