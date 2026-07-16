-- V2: Colleges and departments.
--
-- Soft delete: `deleted_at`/`deleted_by` added per architecture decision (business
-- entities should retain history, not lose it on delete). `deleted_by` intentionally
-- has NO foreign key constraint here — `users` doesn't exist until V3, and constraining
-- it later across colleges/departments/users/students/etc. would tangle migration order
-- for no real benefit. It's treated as a soft reference, validated at the application
-- layer (the service setting it always has a real authenticated user's id in hand).
-- Same pattern is used consistently on every soft-deletable table in this migration set.

CREATE TABLE colleges (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    college_code    VARCHAR(20) NOT NULL,
    address         TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    deleted_by      BIGINT
);
-- Partial unique index, not a plain UNIQUE constraint: a soft-deleted college's code
-- must be free for reuse (e.g. a mistakenly-created college record removed, code reissued).
CREATE UNIQUE INDEX uq_colleges_code ON colleges(college_code) WHERE deleted_at IS NULL;

CREATE TABLE departments (
    id              BIGSERIAL PRIMARY KEY,
    college_id      BIGINT NOT NULL REFERENCES colleges(id),
    name            VARCHAR(255) NOT NULL,
    code            VARCHAR(20) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    deleted_by      BIGINT
);
CREATE UNIQUE INDEX uq_departments_college_code ON departments(college_id, code) WHERE deleted_at IS NULL;
-- Tenant-scoping index: every department lookup in the app is scoped by college_id
-- (a staff/admin never queries departments across colleges) — this is not speculative,
-- it's the direct, immediate consequence of the tenant-aware repository requirement.
CREATE INDEX idx_departments_college ON departments(college_id);
