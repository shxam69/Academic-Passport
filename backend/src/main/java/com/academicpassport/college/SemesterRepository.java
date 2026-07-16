package com.academicpassport.college;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SemesterRepository extends JpaRepository<Semester, Long> {

    Optional<Semester> findByIdAndCollegeId(Long id, Long collegeId);

    List<Semester> findAllByCollegeIdAndDepartmentId(Long collegeId, Long departmentId);

    Optional<Semester> findByDepartmentIdAndSemesterNumber(Long departmentId, Integer semesterNumber);

    @Modifying
    @Query("UPDATE Semester s SET s.deletedAt = CURRENT_TIMESTAMP, s.deletedBy = :deletedBy WHERE s.id = :id AND s.college.id = :collegeId")
    void softDelete(@Param("id") Long id, @Param("collegeId") Long collegeId, @Param("deletedBy") Long deletedBy);
}
