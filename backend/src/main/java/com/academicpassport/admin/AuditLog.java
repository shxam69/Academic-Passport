package com.academicpassport.admin;

import com.academicpassport.auth.User;
import com.academicpassport.college.College;
import com.academicpassport.common.CreatedOnlyEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Append-only action log. No updates, no deletes, ever — that's the entire point
 * of an audit trail. Nothing in this codebase should call save() on an existing
 * AuditLog id; only ever persist new rows.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "audit_logs")
public class AuditLog extends CreatedOnlyEntity {

    // Nullable: some actions are system-initiated (e.g. an OCR pipeline failure)
    // with no human actor.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Nullable: platform-level actions (e.g. SUPER_ADMIN registering a brand new
    // college) have no college context yet at the moment of the action.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "college_id")
    private College college;

    @Column(nullable = false)
    private String action;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadata;
}
