package com.academicpassport.marksheet;

import com.academicpassport.common.IdentifiedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * OCR output for a marksheet. rawOcrJson/validationErrors are stored as plain
 * String, mapped to JSONB via SqlTypes.JSON — the service layer serializes/
 * deserializes with Jackson explicitly rather than mapping to a typed object
 * graph here, which keeps this entity simple and avoids coupling it to the
 * shape of whatever OCR engine's response format Sprint 2 ends up using.
 * <p>
 * rawOcrJson/validationPassed are nullable/default-false respectively because
 * OCR can genuinely fail (corrupt PDF, unreadable scan) — see status/failureReason.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ocr_results")
public class OcrResult extends IdentifiedEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marksheet_id", nullable = false, unique = true)
    private Marksheet marksheet;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "ocr_status")
    private OcrStatus status = OcrStatus.PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_ocr_json", columnDefinition = "jsonb")
    private String rawOcrJson;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "validation_passed", nullable = false)
    private Boolean validationPassed = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_errors", columnDefinition = "jsonb")
    private String validationErrors;

    // Set explicitly by the OCR processing code when it finishes (success or
    // failure), not auto-managed by JPA auditing — the row is created in PENDING
    // before this timestamp is meaningful.
    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "processing_started_at")
    private Instant processingStartedAt;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "findings", columnDefinition = "jsonb")
    private String findings;
}
