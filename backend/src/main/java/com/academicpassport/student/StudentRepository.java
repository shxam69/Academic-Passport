package com.academicpassport.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByIdAndCollegeId(Long id, Long collegeId);

    Optional<Student> findByUserId(Long userId);

    List<Student> findAllByCollegeIdAndDepartmentId(Long collegeId, Long departmentId);

    Optional<Student> findByDepartmentIdAndRollNumber(Long departmentId, String rollNumber);

    boolean existsByUniversityRegisterNo(String universityRegisterNo);

    @Modifying
    @Query("UPDATE Student s SET s.deletedAt = CURRENT_TIMESTAMP, s.deletedBy = :deletedBy WHERE s.id = :id AND s.college.id = :collegeId")
    void softDelete(@Param("id") Long id, @Param("collegeId") Long collegeId, @Param("deletedBy") Long deletedBy);
}
