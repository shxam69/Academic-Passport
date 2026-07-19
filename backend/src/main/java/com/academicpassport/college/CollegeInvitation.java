package com.academicpassport.college;

import com.academicpassport.auth.User;
import com.academicpassport.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "college_invitations")
public class CollegeInvitation extends BaseEntity {

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "institution_name", nullable = false)
    private String institutionName;

    @Column(name = "admin_email", nullable = false)
    private String adminEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CollegeInvitationStatus status = CollegeInvitationStatus.PENDING;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
    
    // Helper to dynamically check expiration
    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }
}
