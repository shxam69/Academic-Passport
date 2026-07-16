package com.academicpassport.support;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    List<SupportTicket> findAllByUserId(Long userId);

    // SUPER_ADMIN-facing queue, per RBAC matrix — same "no tenant filter needed"
    // reasoning as AuditLogRepository would apply if a college-scoped ticket view
    // is ever needed; not built now (YAGNI — nothing in the MVP API contract
    // calls for it), join through user.college.id if that need shows up later.
    Page<SupportTicket> findAllByStatus(TicketStatus status, Pageable pageable);
}
