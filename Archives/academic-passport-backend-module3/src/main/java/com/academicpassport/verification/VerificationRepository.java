package com.academicpassport.verification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VerificationRepository extends JpaRepository<Verification, Long> {

    Optional<Verification> findByMarksheetId(Long marksheetId);

    /**
     * Staff pending-verification queue (GET /staff/queue). Verification has no
     * college_id/department_id of its own — tenant AND department scoping both
     * come from a join through marksheet -> student -> department, which is why
     * this is a JPQL query rather than a derived method name (a derived name for
     * a 3-hop join gets unreadable fast). Both collegeId and departmentId are
     * required params, not optional — per the RBAC/IDOR review, staff must never
     * see another department's queue even within the same college.
     */
    @Query("""
            SELECT v FROM Verification v
            WHERE v.status = com.academicpassport.verification.VerificationStatus.PENDING
              AND v.marksheet.college.id = :collegeId
              AND v.marksheet.student.department.id = :departmentId
            """)
    List<Verification> findPendingQueue(@Param("collegeId") Long collegeId, @Param("departmentId") Long departmentId);
}
