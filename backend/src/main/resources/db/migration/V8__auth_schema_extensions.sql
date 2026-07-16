-- Support proper refresh token family revocation
ALTER TABLE refresh_tokens ADD COLUMN family_id VARCHAR(36);
-- Populate existing rows (if any) with their own ID as family to satisfy constraint, then make NOT NULL
UPDATE refresh_tokens SET family_id = id::varchar WHERE family_id IS NULL;
ALTER TABLE refresh_tokens ALTER COLUMN family_id SET NOT NULL;
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens(family_id);

-- Add email verification tokens
CREATE TABLE email_verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_email_verification_tokens_user ON email_verification_tokens(user_id);
CREATE UNIQUE INDEX uq_email_verification_tokens_hash ON email_verification_tokens(token_hash);
CREATE INDEX idx_email_verification_tokens_expires ON email_verification_tokens(expires_at);
