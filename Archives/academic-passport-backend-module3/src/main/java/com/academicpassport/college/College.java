package com.academicpassport.college;

import com.academicpassport.common.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/**
 * The tenant root. Every other tenant-scoped entity ultimately traces back to a
 * College, either directly (departments, users, students, staff, semesters,
 * subjects, marksheets all carry college_id) or via a relationship.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "colleges")
@SQLRestriction("deleted_at IS NULL")
public class College extends SoftDeletableEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "college_code", nullable = false)
    private String collegeCode;

    @Column
    private String address;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
