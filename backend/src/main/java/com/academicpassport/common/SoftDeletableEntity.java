package com.academicpassport.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Base for the 8 entities with an approved soft-delete strategy: colleges,
 * departments, users, students, staff, semesters, subjects, marksheets.
 * <p>
 * IMPORTANT — coding standard, enforce in code review: soft-deletable entities
 * must NEVER be removed via {@code repository.delete(entity)} /
 * {@code deleteById(id)}. Those issue a real SQL DELETE and bypass this column
 * entirely. Every repository for a soft-deletable entity exposes an explicit
 * {@code softDelete(id, deletedBy)} method instead (see e.g.
 * {@link com.academicpassport.college.CollegeRepository}) — that is the only
 * sanctioned way to remove one of these rows.
 * <p>
 * {@code @SQLRestriction} (Hibernate's successor to the deprecated {@code @Where}
 * annotation) is applied per-entity, not here on the superclass, because Hibernate
 * requires the restriction's column reference to resolve against the concrete
 * entity/table, not the mapped superclass.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class SoftDeletableEntity extends BaseEntity {

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // Deliberately NOT a @ManyToOne to User here — see the V2 migration comment:
    // this is an intentional soft reference with no FK constraint, to avoid
    // migration-ordering problems (colleges/departments precede users). Kept as a
    // plain Long, validated at the service layer where the actor's real user id
    // is already known from the authenticated request.
    @Column(name = "deleted_by")
    private Long deletedBy;

    @Transient
    public boolean isDeleted() {
        return deletedAt != null;
    }
}
