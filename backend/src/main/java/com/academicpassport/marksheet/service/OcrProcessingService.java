package com.academicpassport.marksheet.service;

import com.academicpassport.marksheet.ExtractionFinding;
import com.academicpassport.marksheet.Marksheet;
import com.academicpassport.marksheet.MarksheetRepository;
import com.academicpassport.marksheet.MarksheetSubject;
import com.academicpassport.marksheet.MarksheetSubjectRepository;
import com.academicpassport.marksheet.OcrResult;
import com.academicpassport.marksheet.OcrResultRepository;
import com.academicpassport.marksheet.OcrStatus;
import com.academicpassport.marksheet.dto.ExtractionContext;
import com.academicpassport.marksheet.dto.ExtractionResult;
import com.academicpassport.marksheet.provider.ExtractionException;
import com.academicpassport.marksheet.provider.MarksheetExtractionProvider;
import com.academicpassport.storage.FileStorageService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class OcrProcessingService {

    private static final Logger log = LoggerFactory.getLogger(OcrProcessingService.class);

    private final OcrResultRepository ocrResultRepository;
    private final MarksheetRepository marksheetRepository;
    private final MarksheetSubjectRepository marksheetSubjectRepository;
    private final MarksheetExtractionProvider extractionProvider;
    private final FileStorageService storageService;
    private final OcrValidationService ocrValidationService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public OcrProcessingService(OcrResultRepository ocrResultRepository,
                                MarksheetRepository marksheetRepository,
                                MarksheetSubjectRepository marksheetSubjectRepository,
                                MarksheetExtractionProvider extractionProvider,
                                FileStorageService storageService,
                                OcrValidationService ocrValidationService,
                                ObjectMapper objectMapper,
                                TransactionTemplate transactionTemplate) {
        this.ocrResultRepository = ocrResultRepository;
        this.marksheetRepository = marksheetRepository;
        this.marksheetSubjectRepository = marksheetSubjectRepository;
        this.extractionProvider = extractionProvider;
        this.storageService = storageService;
        this.ocrValidationService = ocrValidationService;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
    }

    public void claimAndProcess(Long marksheetId) {
        // Atomic claim (uses TransactionTemplate to commit immediately)
        Boolean claimed = transactionTemplate.execute(status -> {
            int updated = ocrResultRepository.claimForProcessing(marksheetId);
            return updated > 0;
        });

        if (claimed == null || !claimed) {
            log.info("Could not claim marksheet {} for processing (already processing or not pending)", marksheetId);
            return;
        }

        log.info("Successfully claimed marksheet {} for processing", marksheetId);
        
        try {
            processDocument(marksheetId);
        } catch (Exception e) {
            log.error("Error during OCR processing for marksheet {}", marksheetId, e);
            handleProcessingFailure(marksheetId, e);
        }
    }

    private void processDocument(Long marksheetId) {
        // Load data in read-only transaction or normally
        Marksheet marksheet = transactionTemplate.execute(s -> {
            Marksheet m = marksheetRepository.findById(marksheetId).orElse(null);
            if (m != null) {
                org.hibernate.Hibernate.initialize(m.getStudent());
                org.hibernate.Hibernate.initialize(m.getSemester());
                org.hibernate.Hibernate.initialize(m.getCollege());
            }
            return m;
        });
        if (marksheet == null) return;
        
        // Construct context
        ExtractionContext context = ExtractionContext.builder()
            .fileKey(marksheet.getFileKey())
            .expectedStudentName(marksheet.getStudent().getFullName())
            .expectedRegisterNumber(marksheet.getStudent().getUniversityRegisterNo())
            .expectedSemesterNumber(marksheet.getSemester().getSemesterNumber())
            .expectedInstitutionName(marksheet.getCollege().getName())
            .build();
            
        Resource document = storageService.loadFileAsResource(marksheet.getFileKey());
        
        // EXTERNAL API CALL (Not holding DB transaction)
        ExtractionResult result = extractionProvider.extract(document, context);
        
        // Validate and match (In Memory)
        OcrValidationResult validation = ocrValidationService.validateAndMatch(result, marksheet);
        
        // Transactional Save
        transactionTemplate.executeWithoutResult(status -> {
            saveExtractionResult(marksheet, result, validation);
        });
    }

    private void saveExtractionResult(Marksheet marksheet, ExtractionResult result, OcrValidationResult validation) {
        OcrResult ocrResult = ocrResultRepository.findByMarksheetId(marksheet.getId())
            .orElseThrow(() -> new IllegalStateException("OcrResult not found"));
            
        // 1. Delete previous candidate subjects
        marksheetSubjectRepository.deleteByMarksheetId(marksheet.getId());
        
        // 2. Save new subjects
        for (OcrValidationResult.MatchedSubject match : validation.getMatchedSubjects()) {
            MarksheetSubject ms = new MarksheetSubject();
            ms.setMarksheet(marksheet);
            ms.setSubject(match.getExpectedSubject());
            ms.setAiExtractedMarks(match.getExtracted().getMarksObtained());
            ms.setCorrectedMarks(match.getExtracted().getMarksObtained());
            ms.setGrade(match.getExtracted().getGrade());
            ms.setIsEditedByStudent(false);
            ms.setIsEditedByStaff(false);
            marksheetSubjectRepository.save(ms);
        }
        
        // 3. Update OcrResult
        try {
            ocrResult.setRawOcrJson(objectMapper.writeValueAsString(result));
            ocrResult.setFindings(objectMapper.writeValueAsString(validation.getFindings()));
        } catch (JacksonException e) {
            log.error("Failed to serialize OCR JSON", e);
        }
        
        ocrResult.setConfidenceScore(validation.getConfidenceScore());
        ocrResult.setValidationPassed(validation.isReviewRequired() ? false : true);
        ocrResult.setStatus(OcrStatus.COMPLETED);
        ocrResult.setProcessedAt(Instant.now());
        ocrResult.setFailureReason(null);
        
        ocrResultRepository.save(ocrResult);
    }

    private void handleProcessingFailure(Long marksheetId, Exception e) {
        transactionTemplate.executeWithoutResult(status -> {
            OcrResult ocrResult = ocrResultRepository.findByMarksheetId(marksheetId).orElse(null);
            if (ocrResult == null) return;
            
            boolean permanent = false;
            if (e instanceof ExtractionException) {
                permanent = ((ExtractionException) e).isPermanent();
            }
            
            if (permanent || ocrResult.getAttemptCount() >= 3) {
                ocrResult.setStatus(OcrStatus.FAILED);
            } else {
                ocrResult.setStatus(OcrStatus.FAILED_RETRYABLE);
                ocrResult.setNextRetryAt(Instant.now().plus(5, ChronoUnit.MINUTES));
            }
            
            ocrResult.setFailureReason(e.getMessage());
            ocrResult.setProcessedAt(Instant.now());
            ocrResultRepository.save(ocrResult);
        });
    }
}
