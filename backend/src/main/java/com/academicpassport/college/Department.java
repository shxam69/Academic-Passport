package com.academicpassport.college;

import com.academicpassport.common.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "departments")
@SQLRestriction("deleted_at IS NULL")
public class Department extends SoftDeletableEntity {

    // FetchType.LAZY everywhere for @ManyToOne/@OneToOne in this codebase, not just
    // here — eager fetching by default is one of the more common ways a Spring Data
    // JPA app quietly turns into an N+1 query generator. Load what's needed
    // explicitly (via JOIN FETCH in a repository query) rather than implicitly.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "college_id", nullable = false)
    private College college;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String code;
}
