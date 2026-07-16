package com.academicpassport.auth;

import com.academicpassport.college.College;
import com.academicpassport.common.VersionedSoftDeletableEntity;
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

/**
 * Login identity for STUDENT, STAFF, and SUPER_ADMIN.
 * <p>
 * IMPORTANT: the DB enforces {@code chk_super_admin_no_college} — SUPER_ADMIN must
 * have a null college, every other role must have a non-null one. JPA/Bean
 * Validation has no clean way to express "this field's nullability depends on
 * another field's value" as a single annotation, so this invariant is NOT
 * re-validated here at the entity level — it's enforced by the DB constraint
 * (which will reject an insert/update that violates it) and should also be
 * validated explicitly in the auth service before persisting (Module 5), so the
 * user gets a clean 400 instead of a raw constraint-violation exception surfacing
 * from the DB layer.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
@SQLRestriction("deleted_at IS NULL")
public class User extends VersionedSoftDeletableEntity {

    // Nullable by design — null only for SUPER_ADMIN. See class-level javadoc.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "college_id")
    private College college;

    @Column(nullable = false)
    private String email;

    @Column
    private String mobile;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "role", nullable = false, columnDefinition = "user_role")
    private UserRole role;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;
}
