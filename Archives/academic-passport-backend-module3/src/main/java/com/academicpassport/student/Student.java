package com.academicpassport.student;

import com.academicpassport.auth.User;
import com.academicpassport.college.College;
import com.academicpassport.college.Department;
import com.academicpassport.common.VersionedSoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "students")
@SQLRestriction("deleted_at IS NULL")
public class Student extends VersionedSoftDeletableEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "college_id", nullable = false)
    private College college;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "roll_number", nullable = false)
    private String rollNumber;

    @Column(name = "university_register_no", nullable = false)
    private String universityRegisterNo;

    @Column(nullable = false)
    private LocalDate dob;

    @Column
    private String section;

    @Column(name = "batch_year", nullable = false)
    private Integer batchYear;

    @Column(name = "current_semester", nullable = false)
    private Integer currentSemester = 1;

    @Column
    private String shift;
}
