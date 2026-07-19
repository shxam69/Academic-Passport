package com.academicpassport.college;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    // Tenant-aware by construction: every finder takes collegeId, not just id.
    // Service layer must always pass the authenticated user's collegeId — never
    // call findById() bare on this repository for a tenant-scoped read.
    Optional<Department> findByIdAndCollegeId(Long id, Long collegeId);

    List<Department> findAllByCollegeId(Long collegeId);

    Optional<Department> findByCollegeIdAndCode(Long collegeId, String code);

    @Query("SELECT d FROM Department d WHERE d.college.id = :collegeId AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(d.name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR " +
           "LOWER(d.code) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')))")
    Page<Department> searchDepartments(
            @Param("collegeId") Long collegeId, 
            @Param("search") String search, 
            Pageable pageable);

    @Modifying
    @Query("UPDATE Department d SET d.deletedAt = CURRENT_TIMESTAMP, d.deletedBy = :deletedBy WHERE d.id = :id AND d.college.id = :collegeId")
    void softDelete(@Param("id") Long id, @Param("collegeId") Long collegeId, @Param("deletedBy") Long deletedBy);
}
