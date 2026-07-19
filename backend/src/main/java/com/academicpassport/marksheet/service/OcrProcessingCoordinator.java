package com.academicpassport.marksheet.service;

import com.academicpassport.marksheet.event.MarksheetUploadedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OcrProcessingCoordinator {

    private static final Logger log = LoggerFactory.getLogger(OcrProcessingCoordinator.class);
    private final OcrProcessingService ocrProcessingService;

    public OcrProcessingCoordinator(OcrProcessingService ocrProcessingService) {
        this.ocrProcessingService = ocrProcessingService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMarksheetUploadedEvent(MarksheetUploadedEvent event) {
        log.info("Received MarksheetUploadedEvent for Marksheet ID: {}", event.getMarksheetId());
        try {
            // Attempt to claim and process immediately
            ocrProcessingService.claimAndProcess(event.getMarksheetId());
        } catch (Exception e) {
            log.error("Failed to asynchronously process marksheet {}: {}", event.getMarksheetId(), e.getMessage(), e);
            // If it fails here (e.g., unexpected crash during claim), it remains in PENDING.
            // The scheduled fallback worker will pick it up later.
        }
    }
}
