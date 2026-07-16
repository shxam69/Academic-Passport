package com.academicpassport.marksheet;

import com.academicpassport.college.Subject;
import com.academicpassport.common.VersionedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One subject's extracted marks within a marksheet. Versioned because both the
 * student (self-correction during review) and staff (override during
 * verification) can write to marksObtained/grade in quick succession.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "marksheet_subjects")
public class MarksheetSubject extends VersionedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marksheet_id", nullable = false)
    private Marksheet marksheet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "marks_obtained")
    private Integer marksObtained;

    @Column
    private String grade;

    @Column(name = "is_edited_by_student", nullable = false)
    private Boolean isEditedByStudent = false;

    @Column(name = "is_edited_by_staff", nullable = false)
    private Boolean isEditedByStaff = false;
}
