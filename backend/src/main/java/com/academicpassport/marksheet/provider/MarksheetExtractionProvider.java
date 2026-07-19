package com.academicpassport.marksheet.provider;

import com.academicpassport.marksheet.dto.ExtractionContext;
import com.academicpassport.marksheet.dto.ExtractionResult;
import org.springframework.core.io.Resource;

public interface MarksheetExtractionProvider {
    /**
     * Extracts structured academic data from the provided document resource.
     * 
     * @param document The marksheet file resource (PDF, JPEG, PNG)
     * @param context Contextual hints like expected register number, semester, etc.
     * @return Structured ExtractionResult
     * @throws ExtractionException if the provider fails (timeout, unavailable, invalid JSON)
     */
    ExtractionResult extract(Resource document, ExtractionContext context) throws ExtractionException;
}
