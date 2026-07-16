-- V7: Support and audit trail.
--
-- audit_logs.college_id is nullable and denormalized directly (not derived from
-- user_id, since some actions are platform-level with no college context yet — e.g.
-- a SUPER_ADMIN registering a brand new college). Where it IS populated, it lets a
-- future per-college audit view avoid joining through users for every row.
-- No soft delete on any table here: audit_logs is append-only by definition (deleting
-- an audit entry defeats its purpose); notifications and support_tickets have their
-- own lifecycle (is_read / status) that already covers "this is no longer active"
-- without needing a second, overlapping deletion concept.

CREATE TABLE audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES users(id),
    college_id      BIGINT REFERENCES colleges(id),
    action          VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(50),
    entity_id       BIGINT,
    metadata        JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_college ON audit_logs(college_id);

CREATE TABLE notifications (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title           VARCHAR(255) NOT NULL,
    body            TEXT,
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_user ON notifications(user_id);

CREATE TABLE support_tickets (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    subject         VARCHAR(255) NOT NULL,
    description     TEXT,
    status          ticket_status NOT NULL DEFAULT 'OPEN',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_support_tickets_user ON support_tickets(user_id);
CREATE INDEX idx_support_tickets_status ON support_tickets(status);
