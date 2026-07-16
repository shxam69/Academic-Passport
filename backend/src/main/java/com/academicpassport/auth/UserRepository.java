package com.academicpassport.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // STUDENT/STAFF login — tenant-scoped, matches the partial unique index
    // uq_users_college_email.
    Optional<User> findByCollegeIdAndEmail(Long collegeId, String email);

    // SUPER_ADMIN login — deliberately NOT tenant-scoped, since college_id is
    // null for this role by the DB's CHECK constraint. Matches
    // uq_users_super_admin_email.
    Optional<User> findByEmailAndCollegeIdIsNull(String email);

    // Tenant-scoped lookup by id, for STUDENT/STAFF self-service and staff/admin
    // operating on a specific tenant's users.
    Optional<User> findByIdAndCollegeId(Long id, Long collegeId);
    
    // Find all users by email to detect if a collegeCode is required for login
    java.util.List<User> findByEmail(String email);

    // Used by the Module 4 bootstrap CommandLineRunner to check idempotently
    // whether a SUPER_ADMIN already exists before creating one.
    boolean existsByRole(UserRole role);

    // No collegeId filter here (SUPER_ADMIN has none, so a single signature can't
    // cleanly require it for every caller). This means the SERVICE LAYER is fully
    // responsible for verifying the caller may act on this user (ownership/tenant
    // check) before calling softDelete — same IDOR-prevention discipline required
    // on every other tenant-scoped mutation in this codebase.
    @Modifying
    @Query("UPDATE User u SET u.deletedAt = CURRENT_TIMESTAMP, u.deletedBy = :deletedBy WHERE u.id = :id")
    void softDelete(@Param("id") Long id, @Param("deletedBy") Long deletedBy);
}
