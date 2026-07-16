package com.academicpassport.support;

import com.academicpassport.auth.User;
import com.academicpassport.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * The `status` enum column already models the ticket's lifecycle (OPEN ->
 * IN_PROGRESS -> RESOLVED/CLOSED) — no separate soft-delete concept is needed on
 * top of that, and no genuine concurrent-edit risk justifies @Version here
 * (single admin actor progresses a ticket's status, not a multi-writer race).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "support_tickets")
public class SupportTicket extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String subject;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "ticket_status")
    private TicketStatus status = TicketStatus.OPEN;
}
