package com.academicpassport.marksheet;

import com.academicpassport.college.College;
import com.academicpassport.college.Semester;
import com.academicpassport.common.VersionedSoftDeletableEntity;
import com.academicpassport.student.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * One student's upload for one semester. See the V6 migration comment for why
 * the (student_id, semester_id) uniqueness is a PARTIAL index (WHERE deleted_at
 * IS NULL) rather than a plain UNIQUE constraint — it changes what "reupload"
 * means once soft delete is in the picture.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "marksheets")
@SQLRestriction("deleted_at IS NULL")
public class Marksheet extends VersionedSoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "college_id", nullable = false)
    private College college;

    // Object storage key, e.g. S3/MinIO path — never a public URL. Downloads go
    // through a pre-signed URL generated on demand (see API contract), not this
    // field directly.
    @Column(name = "file_key", nullable = false)
    private String fileKey;

    @Column(name = "file_hash", nullable = false)
    private String fileHash;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "virus_scan_status", nullable = false, columnDefinition = "scan_status")
    private ScanStatus virusScanStatus = ScanStatus.PENDING;
}
