package com.academicpassport.marksheet.provider;

import com.academicpassport.marksheet.dto.ExtractionContext;
import com.academicpassport.marksheet.dto.ExtractionResult;
import org.springframework.core.io.Resource;

import java.util.ArrayList;

public class MockExtractionProvider implements MarksheetExtractionProvider {
    @Override
    public ExtractionResult extract(Resource document, ExtractionContext context) throws ExtractionException {
        // Return a perfectly matching mock extraction for tests
        ExtractionResult result = new ExtractionResult();
        result.setStudentName(context.getExpectedStudentName());
        result.setRegisterNumber(context.getExpectedRegisterNumber());
        result.setSemesterNumber(context.getExpectedSemesterNumber());
        result.setInstitutionName(context.getExpectedInstitutionName());
        result.setSubjects(new ArrayList<>());
        
        ExtractionResult.ExtractedSubject sub1 = new ExtractionResult.ExtractedSubject();
        sub1.setSubjectCode("CS101");
        sub1.setSubjectName("Intro to Computer Science");
        sub1.setMarksObtained(85);
        sub1.setTotalMarks(100);
        result.getSubjects().add(sub1);
        
        return result;
    }
}
