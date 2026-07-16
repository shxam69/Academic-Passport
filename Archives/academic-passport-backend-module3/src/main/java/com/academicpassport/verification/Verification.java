package com.academicpassport.verification;

import com.academicpassport.common.VersionedEntity;
import com.academicpassport.marksheet.Marksheet;
import com.academicpassport.staff.Staff;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "verifications")
public class Verification extends VersionedEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marksheet_id", nullable = false, unique = true)
    private Marksheet marksheet;

    // Nullable: a freshly-submitted marksheet has a verification row in PENDING
    // status with no staff assigned yet — staff_id is set only once someone picks
    // it up for review.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "verification_status")
    private VerificationStatus status = VerificationStatus.PENDING;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "verified_at")
    private Instant verifiedAt;
}
