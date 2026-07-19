package com.academicpassport.marksheet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OcrResultRepository extends JpaRepository<OcrResult, Long> {

    // No direct tenant scoping here — OcrResult has no college_id of its own by
    // design (it's a 1:1 child of Marksheet, which is already tenant-scoped).
    // The service layer must load and ownership-check the parent Marksheet
    // (findByIdAndCollegeId) BEFORE calling this — never look up an OcrResult by
    // marksheetId alone and trust it without that prior check, or a student could
    // read another college's OCR data by guessing marksheet ids.
    Optional<OcrResult> findByMarksheetId(Long marksheetId);

    @Modifying
    @Query("UPDATE OcrResult o SET o.status = 'PROCESSING', o.processingStartedAt = CURRENT_TIMESTAMP, o.attemptCount = o.attemptCount + 1 " +
           "WHERE o.marksheet.id = :marksheetId AND o.status IN ('PENDING', 'FAILED_RETRYABLE')")
    int claimForProcessing(@Param("marksheetId") Long marksheetId);
    
    @Query(value = "SELECT marksheet_id FROM ocr_results " +
                   "WHERE status IN ('PENDING', 'FAILED_RETRYABLE') " +
                   "AND (next_retry_at IS NULL OR next_retry_at <= CURRENT_TIMESTAMP) " +
                   "FOR UPDATE SKIP LOCKED LIMIT :limit", nativeQuery = true)
    List<Long> findStuckJobs(@Param("limit") int limit);
}
