package com.academicpassport.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * For entities whose table has BOTH created_at and updated_at columns:
 * colleges, departments, users, students, staff, semesters, subjects,
 * marksheets, support_tickets.
 * <p>
 * Deliberately NOT used for every entity — several tables only track created_at
 * (refresh_tokens, password_reset_tokens, audit_logs, notifications — see
 * {@link CreatedOnlyEntity}), and two have neither and rely on their own
 * domain-specific timestamp instead (ocr_results.processed_at, verifications
 * .verified_at — see {@link IdentifiedEntity}). Forcing every entity through
 * this class would mean either adding meaningless updated_at columns to
 * append-only/immutable tables, or having ddl-auto=validate fail at startup.
 * Neither is worth it just to keep one class hierarchy uniform.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity extends IdentifiedEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
