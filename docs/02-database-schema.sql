-- Academic Passport — MVP Database Schema (PostgreSQL)
-- Design notes:
--   - Every table that will eventually need tenant scoping has college_id now.
--     No tenant-isolation logic (RLS, per-tenant middleware) is built yet — that's Phase 2+,
--     added when college #2 signs on. One real tenant does not justify that complexity today.
--   - No separate "permissions" table. Roles are an enum on `users`. If you need
--     fine-grained permissions later, add a permissions table then — don't build it speculatively.
--   - No separate `sessions` table. JWT access token (short-lived) + refresh_tokens
--     (rotating, hashed at rest) is the single session strategy.
--   - Payments/subscriptions tables intentionally omitted from MVP — Phase 2.

CREATE TYPE user_role AS ENUM ('STUDENT', 'STAFF', 'SUPER_ADMIN');
CREATE TYPE verification_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');
CREATE TYPE ticket_status AS ENUM ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED');
CREATE TYPE scan_status AS ENUM ('PENDING', 'CLEAN', 'INFECTED');
CREATE TYPE ocr_status AS ENUM ('PENDING', 'COMPLETED', 'FAILED');

-- ============================================================
-- CORE IDENTITY
-- ============================================================

CREATE TABLE colleges (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    college_code    VARCHAR(20) UNIQUE NOT NULL,   -- used at student registration
    address         TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE departments (
    id              BIGSERIAL PRIMARY KEY,
    college_id      BIGINT NOT NULL REFERENCES colleges(id),
    name            VARCHAR(255) NOT NULL,
    code            VARCHAR(20) NOT NULL,
    UNIQUE (college_id, code)
);

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    college_id      BIGINT REFERENCES colleges(id),  -- NULL for SUPER_ADMIN only; platform-level, not tied to one college
    email           VARCHAR(255) NOT NULL,
    mobile          VARCHAR(15),
    password_hash   VARCHAR(255) NOT NULL,
    role            user_role NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    is_verified     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_super_admin_no_college CHECK (
        (role = 'SUPER_ADMIN' AND college_id IS NULL) OR
        (role != 'SUPER_ADMIN' AND college_id IS NOT NULL)
    )
);
-- Email uniqueness is per-college for STUDENT/STAFF (same email could legitimately
-- exist at two colleges); SUPER_ADMIN has no college_id so needs its own index.
CREATE UNIQUE INDEX uq_users_college_email ON users(college_id, email) WHERE college_id IS NOT NULL;
CREATE UNIQUE INDEX uq_users_super_admin_email ON users(email) WHERE role = 'SUPER_ADMIN';

CREATE TABLE refresh_tokens (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash      VARCHAR(255) NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Password reset — promoted from "add later" to required. Students will forget
-- passwords within weeks of real usage; this is not a deferrable nicety.
CREATE TABLE password_reset_tokens (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash      VARCHAR(255) NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,   -- short-lived, e.g. 30 min
    used            BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- STUDENT / STAFF PROFILES
-- ============================================================

CREATE TABLE students (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    department_id           BIGINT NOT NULL REFERENCES departments(id),
    full_name               VARCHAR(255) NOT NULL,
    roll_number              VARCHAR(50) NOT NULL,
    university_register_no  VARCHAR(50) NOT NULL,
    dob                     DATE NOT NULL,
    section                 VARCHAR(10),
    batch_year              INT NOT NULL,          -- year of admission
    current_semester        INT NOT NULL DEFAULT 1,
    shift                   VARCHAR(10),
    UNIQUE (department_id, roll_number),
    UNIQUE (university_register_no)   -- issued by the university, must be globally unique; was missing
);

CREATE TABLE staff (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    department_id   BIGINT NOT NULL REFERENCES departments(id),
    full_name       VARCHAR(255) NOT NULL,
    designation     VARCHAR(100)
);

-- ============================================================
-- ACADEMIC STRUCTURE
-- ============================================================

CREATE TABLE semesters (
    id              BIGSERIAL PRIMARY KEY,
    department_id   BIGINT NOT NULL REFERENCES departments(id),
    semester_number INT NOT NULL,
    UNIQUE (department_id, semester_number)
);

CREATE TABLE subjects (
    id              BIGSERIAL PRIMARY KEY,
    semester_id     BIGINT NOT NULL REFERENCES semesters(id),
    subject_code    VARCHAR(20) NOT NULL,
    subject_name    VARCHAR(255) NOT NULL,
    max_marks       INT NOT NULL DEFAULT 100,
    UNIQUE (semester_id, subject_code)
);

-- ============================================================
-- MARKSHEET UPLOAD / OCR / VERIFICATION
-- ============================================================

-- NOTE on the UNIQUE(student_id, semester_id) constraint below: this is a deliberate
-- choice, not an oversight. On REJECT, the student re-uploads through the SAME row
-- (PUT /marksheets/{id}/reupload — see API contract), which overwrites file_key/file_hash,
-- resets ocr_results, and flips the verification back to PENDING. This avoids building
-- full version history for MVP. The tradeoff: the original rejected file isn't retained
-- in `marksheets` after a reupload — but the rejection event itself (who, when, why) is
-- preserved in audit_logs, so nothing is silently lost. Full versioning is 🟡 Phase 2 if
-- staff need to compare multiple submission attempts side by side.
CREATE TABLE marksheets (
    id              BIGSERIAL PRIMARY KEY,
    student_id      BIGINT NOT NULL REFERENCES students(id),
    semester_id     BIGINT NOT NULL REFERENCES semesters(id),
    file_key        VARCHAR(500) NOT NULL,   -- object storage key, never a public URL
    file_hash       VARCHAR(128) NOT NULL,   -- for dedupe / integrity check
    uploaded_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    virus_scan_status scan_status NOT NULL DEFAULT 'PENDING',
    UNIQUE (student_id, semester_id)
);

CREATE TABLE ocr_results (
    id                  BIGSERIAL PRIMARY KEY,
    marksheet_id        BIGINT NOT NULL UNIQUE REFERENCES marksheets(id) ON DELETE CASCADE,
    status               ocr_status NOT NULL DEFAULT 'PENDING',
    raw_ocr_json         JSONB,                    -- NULL if OCR failed outright (corrupt PDF, unreadable scan)
    failure_reason        TEXT,                    -- set when status = FAILED
    confidence_score     NUMERIC(4,3),             -- 0.000–1.000, from OCR engine
    validation_passed    BOOLEAN NOT NULL DEFAULT FALSE,  -- regex/rule checks
    validation_errors    JSONB,                    -- list of rule failures, if any
    processed_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- On FAILED: student sees "we couldn't read this file, please re-upload a clearer scan"
-- and can enter values manually as a fallback — don't let OCR failure be a dead end.

-- Per-subject extracted marks, editable by student (pre-submit) then locked at staff approval
CREATE TABLE marksheet_subjects (
    id              BIGSERIAL PRIMARY KEY,
    marksheet_id    BIGINT NOT NULL REFERENCES marksheets(id) ON DELETE CASCADE,
    subject_id      BIGINT NOT NULL REFERENCES subjects(id),
    marks_obtained  INT,
    grade           VARCHAR(5),
    is_edited_by_student BOOLEAN NOT NULL DEFAULT FALSE,
    is_edited_by_staff   BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (marksheet_id, subject_id)
);

CREATE TABLE verifications (
    id              BIGSERIAL PRIMARY KEY,
    marksheet_id    BIGINT NOT NULL UNIQUE REFERENCES marksheets(id) ON DELETE CASCADE,
    staff_id        BIGINT REFERENCES staff(id),
    status          verification_status NOT NULL DEFAULT 'PENDING',
    rejection_reason TEXT,
    verified_at     TIMESTAMPTZ
);

-- ============================================================
-- SUPPORT / AUDIT / NOTIFICATIONS
-- ============================================================

CREATE TABLE audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES users(id),
    action          VARCHAR(100) NOT NULL,   -- e.g. 'MARKSHEET_APPROVED'
    entity_type     VARCHAR(50),
    entity_id       BIGINT,
    metadata        JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE notifications (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title           VARCHAR(255) NOT NULL,
    body            TEXT,
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE support_tickets (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    subject         VARCHAR(255) NOT NULL,
    description     TEXT,
    status          ticket_status NOT NULL DEFAULT 'OPEN',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- INDEXES (MVP-critical only)
-- ============================================================

CREATE INDEX idx_students_department ON students(department_id);
CREATE INDEX idx_marksheets_student ON marksheets(student_id);
CREATE INDEX idx_verifications_status ON verifications(status);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);

-- ============================================================
-- DEFERRED TO PHASE 2+ (do not build now)
-- ============================================================
-- subscriptions, payments        -> Phase 2, once pilot validates + college agrees to pay
-- courses (as distinct from department) -> only needed if one department spans multiple degree programs; YAGNI for pilot
-- permissions table              -> add only if role enum stops being sufficient
-- tenant isolation (RLS, schema-per-tenant) -> add when college #2 signs on
