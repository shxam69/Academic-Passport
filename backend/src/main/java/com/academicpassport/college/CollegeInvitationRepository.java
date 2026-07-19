package com.academicpassport.college;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CollegeInvitationRepository extends JpaRepository<CollegeInvitation, Long> {

    Optional<CollegeInvitation> findByTokenHash(String tokenHash);

    List<CollegeInvitation> findByAdminEmailAndStatus(String adminEmail, CollegeInvitationStatus status);

    @Query("SELECT i FROM CollegeInvitation i WHERE i.status = :status OR :status IS NULL ORDER BY i.createdAt DESC")
    Page<CollegeInvitation> findByStatus(@Param("status") CollegeInvitationStatus status, Pageable pageable);
}
