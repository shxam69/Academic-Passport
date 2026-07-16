package com.academicpassport.marksheet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MarksheetRepository extends JpaRepository<Marksheet, Long> {

    Optional<Marksheet> findByIdAndCollegeId(Long id, Long collegeId);

    // Backs the reupload-vs-fresh-upload decision (PUT /marksheets/{id}/reupload
    // vs POST /marksheets): @SQLRestriction already excludes soft-deleted rows,
    // so a present result here means "this student has an active marksheet slot
    // for this semester already" — the service layer uses that to decide whether
    // POST should 409 (already exists, use reupload instead) or proceed.
    Optional<Marksheet> findByStudentIdAndSemesterId(Long studentId, Long semesterId);

    List<Marksheet> findAllByCollegeIdAndStudentId(Long collegeId, Long studentId);

    @Modifying
    @Query("UPDATE Marksheet m SET m.deletedAt = CURRENT_TIMESTAMP, m.deletedBy = :deletedBy WHERE m.id = :id AND m.college.id = :collegeId")
    void softDelete(@Param("id") Long id, @Param("collegeId") Long collegeId, @Param("deletedBy") Long deletedBy);
}
