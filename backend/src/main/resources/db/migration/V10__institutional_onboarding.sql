-- V10: Institutional Onboarding Schema

-- We use persistent statuses for invitations. EXPIRED is determined dynamically.
CREATE TABLE college_invitations (
    id BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE, -- SHA-256 hash of the secure token
    institution_name VARCHAR(255) NOT NULL,
    admin_email VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, USED, REVOKED
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by BIGINT REFERENCES users(id) -- Super Admin who generated it
);

-- Optimize lookup by hash
CREATE INDEX idx_college_invitations_hash ON college_invitations(token_hash);
-- Optimize lookup by email to handle duplicate/replacement logic
CREATE INDEX idx_college_invitations_email ON college_invitations(admin_email);

-- Add structured fields to colleges for onboarding profiles
ALTER TABLE colleges
    ADD COLUMN address_line VARCHAR(255),
    ADD COLUMN city VARCHAR(100),
    ADD COLUMN state VARCHAR(100),
    ADD COLUMN postal_code VARCHAR(20),
    ADD COLUMN country VARCHAR(100),
    ADD COLUMN contact_name VARCHAR(255),
    ADD COLUMN contact_email VARCHAR(255),
    ADD COLUMN contact_phone VARCHAR(50),
    ADD COLUMN institution_type VARCHAR(100),
    ADD COLUMN website VARCHAR(255),
    ADD COLUMN logo_url VARCHAR(500);

-- All newly onboarded colleges via the canonical service will be ACTIVE immediately, 
-- but if we want to track the onboarding origin or approval state later, we could.
-- For MVP, they become ACTIVE (is_active = true) directly.
