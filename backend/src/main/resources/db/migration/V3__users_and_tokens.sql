-- V3: Users and their auth-adjacent tokens.
--
-- `version` added to `users` (optimistic locking): profile/password updates can
-- race between the user themselves and an admin action; a lost-update here is a
-- real, if rare, security-relevant bug (e.g. a password reset silently clobbered
-- by a concurrent profile edit), so it's worth the column even at pilot scale.

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    college_id      BIGINT REFERENCES colleges(id),  -- NULL for SUPER_ADMIN only
    email           VARCHAR(255) NOT NULL,
    mobile          VARCHAR(15),
    password_hash   VARCHAR(255) NOT NULL,
    role            user_role NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    is_verified     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    deleted_by      BIGINT,
    version         BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_super_admin_no_college CHECK (
        (role = 'SUPER_ADMIN' AND college_id IS NULL) OR
        (role != 'SUPER_ADMIN' AND college_id IS NOT NULL)
    )
);
CREATE UNIQUE INDEX uq_users_college_email
    ON users(college_id, email) WHERE college_id IS NOT NULL AND deleted_at IS NULL;
CREATE UNIQUE INDEX uq_users_super_admin_email
    ON users(email) WHERE role = 'SUPER_ADMIN' AND deleted_at IS NULL;
CREATE INDEX idx_users_college ON users(college_id);

-- Ephemeral security artifacts — no soft delete (nothing to preserve; a revoked/expired
-- token has no historical value), no optimistic locking (single-writer lifecycle: issued
-- once, revoked once, never contested-edited).
CREATE TABLE refresh_tokens (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash      VARCHAR(255) NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE UNIQUE INDEX uq_refresh_tokens_hash ON refresh_tokens(token_hash);

CREATE TABLE password_reset_tokens (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash      VARCHAR(255) NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    used            BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens(user_id);
CREATE UNIQUE INDEX uq_password_reset_tokens_hash ON password_reset_tokens(token_hash);
