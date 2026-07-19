package com.academicpassport.marksheet.service;

import com.academicpassport.marksheet.OcrResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OcrFallbackWorker {

    private static final Logger log = LoggerFactory.getLogger(OcrFallbackWorker.class);

    private final OcrResultRepository ocrResultRepository;
    private final OcrProcessingService ocrProcessingService;

    public OcrFallbackWorker(OcrResultRepository ocrResultRepository, OcrProcessingService ocrProcessingService) {
        this.ocrResultRepository = ocrResultRepository;
        this.ocrProcessingService = ocrProcessingService;
    }

    /**
     * Runs every minute to find OCR jobs that are stuck.
     * We don't have a direct findStuckJobs method yet, so we will use JPA or native query.
     */
    @Scheduled(fixedDelay = 60000)
    public void processStuckJobs() {
        List<Long> stuckJobIds = ocrResultRepository.findStuckJobs(10);
        if (!stuckJobIds.isEmpty()) {
            log.info("Found {} stuck OCR jobs to process", stuckJobIds.size());
            for (Long marksheetId : stuckJobIds) {
                try {
                    ocrProcessingService.claimAndProcess(marksheetId);
                } catch (Exception e) {
                    log.error("Fallback worker failed to process marksheet {}", marksheetId, e);
                }
            }
        }
    }
}
