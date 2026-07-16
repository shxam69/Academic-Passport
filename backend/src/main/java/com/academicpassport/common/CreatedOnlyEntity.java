package com.academicpassport.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * For entities whose table tracks created_at only: refresh_tokens,
 * password_reset_tokens, audit_logs, notifications. All four are
 * write-once-then-flag-flipped (revoked/used/is_read) or genuinely
 * append-only (audit_logs) — there's no second timestamp worth tracking.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class CreatedOnlyEntity extends IdentifiedEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
