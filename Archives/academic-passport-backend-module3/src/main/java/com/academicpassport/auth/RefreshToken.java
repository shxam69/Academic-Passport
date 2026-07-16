package com.academicpassport.auth;

import com.academicpassport.common.CreatedOnlyEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * No @SQLRestriction/soft-delete here — a revoked or expired refresh token has no
 * historical value worth preserving, and the `revoked` flag already fully captures
 * its lifecycle. Cleanup of old rows (revoked/expired) is a housekeeping job to
 * add later, not a soft-delete concern.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends CreatedOnlyEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Never store the raw token — only its hash. The service layer hashes the
    // token before ever calling a repository method involving this column.
    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Boolean revoked = false;
}
