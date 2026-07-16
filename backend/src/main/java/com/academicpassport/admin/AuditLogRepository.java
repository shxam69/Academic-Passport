package com.academicpassport.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // No softDelete/delete method exposed deliberately — see AuditLog javadoc.
    // SUPER_ADMIN only, per RBAC matrix; not tenant-restricted to a single college
    // since a super admin's audit view spans every college on the platform, but
    // the collegeId column lets a future "view logs for college X" filter exist
    // without a join.
    Page<AuditLog> findAllByCollegeId(Long collegeId, Pageable pageable);

    Page<AuditLog> findAllByUserId(Long userId, Pageable pageable);
}
