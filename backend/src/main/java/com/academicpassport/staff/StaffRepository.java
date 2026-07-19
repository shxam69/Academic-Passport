package com.academicpassport.staff;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("SELECT s FROM Staff s WHERE s.college.id = :collegeId AND " +
           "(:departmentId IS NULL OR s.department.id = :departmentId) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(s.fullName) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
           "LOWER(s.user.email) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')))")
    Page<Staff> searchStaff(
            @Param("collegeId") Long collegeId,
            @Param("departmentId") Long departmentId,
            @Param("search") String search,
            Pageable pageable);

    boolean existsByDepartmentId(Long departmentId);

    @Modifying
    @Query("UPDATE Staff s SET s.deletedAt = CURRENT_TIMESTAMP, s.deletedBy = :deletedBy WHERE s.id = :id AND s.college.id = :collegeId")
    void softDelete(@Param("id") Long id, @Param("collegeId") Long collegeId, @Param("deletedBy") Long deletedBy);
}
