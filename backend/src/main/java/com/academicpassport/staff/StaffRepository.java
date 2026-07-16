package com.academicpassport.staff;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long> {

    Optional<Staff> findByIdAndCollegeId(Long id, Long collegeId);

    Optional<Staff> findByUserId(Long userId);

    List<Staff> findAllByCollegeIdAndDepartmentId(Long collegeId, Long departmentId);

    @Modifying
    @Query("UPDATE Staff s SET s.deletedAt = CURRENT_TIMESTAMP, s.deletedBy = :deletedBy WHERE s.id = :id AND s.college.id = :collegeId")
    void softDelete(@Param("id") Long id, @Param("collegeId") Long collegeId, @Param("deletedBy") Long deletedBy);
}
