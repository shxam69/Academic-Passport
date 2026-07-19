package com.academicpassport.marksheet.service;

import com.academicpassport.college.College;
import com.academicpassport.college.Semester;
import com.academicpassport.college.Subject;
import com.academicpassport.college.SubjectRepository;
import com.academicpassport.marksheet.ExtractionFinding;
import com.academicpassport.marksheet.Marksheet;
import com.academicpassport.marksheet.dto.ExtractionResult;
import com.academicpassport.student.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class OcrValidationServiceTest {

    private SubjectRepository subjectRepository;
    private OcrValidationService ocrValidationService;
    private Marksheet marksheet;
    private College college;
    private Semester semester;
    private Student student;

    @BeforeEach
    void setUp() {
        subjectRepository = Mockito.mock(SubjectRepository.class);
        ocrValidationService = new OcrValidationService(subjectRepository);

        college = new College();
        college.setId(1L);

        semester = new Semester();
        semester.setId(1L);
        semester.setSemesterNumber(1);

        student = new Student();
        student.setUniversityRegisterNo("REG123");
        student.setFullName("John Doe");

        marksheet = new Marksheet();
        marksheet.setCollege(college);
        marksheet.setSemester(semester);
        marksheet.setStudent(student);

        Subject sub1 = new Subject();
        sub1.setId(101L);
        sub1.setSubjectCode("CS101");
        sub1.setSubjectName("Intro to Computer Science");
        sub1.setMaxMarks(100);

        Subject sub2 = new Subject();
        sub2.setId(102L);
        sub2.setSubjectCode("CS102");
        sub2.setSubjectName("Data Structures");
        sub2.setMaxMarks(100);

        when(subjectRepository.findAllByCollegeIdAndSemesterId(1L, 1L))
                .thenReturn(Arrays.asList(sub1, sub2));
    }

    @Test
    void testPerfectMatch() {
        ExtractionResult result = new ExtractionResult();
        result.setRegisterNumber("REG123");
        result.setSemesterNumber(1);

        ExtractionResult.ExtractedSubject es1 = new ExtractionResult.ExtractedSubject();
        es1.setSubjectCode("CS101");
        es1.setSubjectName("Intro to Computer Science");
        es1.setMarksObtained(85);

        ExtractionResult.ExtractedSubject es2 = new ExtractionResult.ExtractedSubject();
        es2.setSubjectCode("CS102");
        es2.setSubjectName("Data Structures");
        es2.setMarksObtained(90);

        result.setSubjects(Arrays.asList(es1, es2));

        OcrValidationResult validation = ocrValidationService.validateAndMatch(result, marksheet);

        assertFalse(validation.isReviewRequired());
        assertEquals(0, validation.getConfidenceScore().compareTo(BigDecimal.valueOf(100.0)));
        assertTrue(validation.getFindings().isEmpty());
        assertEquals(2, validation.getMatchedSubjects().size());
    }

    @Test
    void testMissingRegisterNumberAndSubjects() {
        ExtractionResult result = new ExtractionResult();
        // Register number missing
        result.setSemesterNumber(1);

        ExtractionResult.ExtractedSubject es1 = new ExtractionResult.ExtractedSubject();
        es1.setSubjectCode("CS101");
        es1.setMarksObtained(85);
        result.setSubjects(Arrays.asList(es1)); // Missing CS102

        OcrValidationResult validation = ocrValidationService.validateAndMatch(result, marksheet);

        assertTrue(validation.isReviewRequired());
        assertTrue(validation.getFindings().contains(ExtractionFinding.UNREADABLE_VALUE));
        assertTrue(validation.getFindings().contains(ExtractionFinding.MISSING_EXPECTED_SUBJECT));
        assertTrue(validation.getConfidenceScore().compareTo(BigDecimal.valueOf(100.0)) < 0);
    }
}
