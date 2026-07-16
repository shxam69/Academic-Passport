-- V6: Marksheet upload, OCR output, extracted marks, and staff verification.
--
-- marksheets: soft-deletable (admin may need to remove a fraudulent/erroneous upload
-- without destroying the audit trail) + versioned (student and staff can both touch
-- fields on this row's lifecycle in quick succession during review).
--
-- The UNIQUE(student_id, semester_id) constraint is now partial (WHERE deleted_at IS
-- NULL), which changes the reupload story from the original design: a rejected
-- marksheet is still updated in place via PUT /marksheets/{id}/reupload (no version
-- history), but if an admin ever soft-deletes a marksheet outright, the student can
-- upload a fresh one for that semester without being blocked by a "unique" row that's
-- actually just sitting there deleted.
CREATE TABLE marksheets (
    id                  BIGSERIAL PRIMARY KEY,
    student_id          BIGINT NOT NULL REFERENCES students(id),
    semester_id         BIGINT NOT NULL REFERENCES semesters(id),
    college_id          BIGINT NOT NULL REFERENCES colleges(id),
    file_key            VARCHAR(500) NOT NULL,
    file_hash           VARCHAR(128) NOT NULL,
    uploaded_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    virus_scan_status   scan_status NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMPTZ,
    deleted_by          BIGINT,
    version             BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_marksheets_student_semester
    ON marksheets(student_id, semester_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_marksheets_student ON marksheets(student_id);
CREATE INDEX idx_marksheets_college ON marksheets(college_id);

-- No soft delete / no version: lifecycle is entirely owned by its parent marksheet
-- (ON DELETE CASCADE if the marksheet row is ever hard-deleted by a superadmin
-- maintenance operation; ordinary soft-delete of the marksheet just hides both via
-- the marksheet's own deleted_at, no cascade needed for that path).
CREATE TABLE ocr_results (
    id                  BIGSERIAL PRIMARY KEY,
    marksheet_id        BIGINT NOT NULL UNIQUE REFERENCES marksheets(id) ON DELETE CASCADE,
    status              ocr_status NOT NULL DEFAULT 'PENDING',
    raw_ocr_json        JSONB,
    failure_reason      TEXT,
    confidence_score    NUMERIC(4,3),
    validation_passed   BOOLEAN NOT NULL DEFAULT FALSE,
    validation_errors   JSONB,
    processed_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Versioned: both student (self-correction) and staff (override) can write to this
-- row's marks_obtained/grade during the review window — genuine concurrent-edit risk.
CREATE TABLE marksheet_subjects (
    id                      BIGSERIAL PRIMARY KEY,
    marksheet_id            BIGINT NOT NULL REFERENCES marksheets(id) ON DELETE CASCADE,
    subject_id              BIGINT NOT NULL REFERENCES subjects(id),
    marks_obtained          INT,
    grade                   VARCHAR(5),
    is_edited_by_student    BOOLEAN NOT NULL DEFAULT FALSE,
    is_edited_by_staff      BOOLEAN NOT NULL DEFAULT FALSE,
    version                 BIGINT NOT NULL DEFAULT 0,
    UNIQUE (marksheet_id, subject_id)
);
CREATE INDEX idx_marksheet_subjects_marksheet ON marksheet_subjects(marksheet_id);

-- Versioned: guards against a double-submit race (e.g. a double-click on Approve
-- firing two requests) silently double-processing the same verification decision.
CREATE TABLE verifications (
    id                  BIGSERIAL PRIMARY KEY,
    marksheet_id        BIGINT NOT NULL UNIQUE REFERENCES marksheets(id) ON DELETE CASCADE,
    staff_id            BIGINT REFERENCES staff(id),
    status              verification_status NOT NULL DEFAULT 'PENDING',
    rejection_reason    TEXT,
    verified_at         TIMESTAMPTZ,
    version             BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_verifications_status ON verifications(status);
CREATE INDEX idx_verifications_staff ON verifications(staff_id);
