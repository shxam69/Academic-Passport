-- V5: Academic structure (semesters, subjects).
--
-- Soft delete matters more here than almost anywhere else in the schema: a subject
-- that gets renamed/retired in the curriculum must NOT be hard-deleted if any
-- marksheet_subjects row (V6) references it — that would corrupt an already-verified
-- academic record. Soft delete lets the curriculum evolve without ever breaking history.

CREATE TABLE semesters (
    id              BIGSERIAL PRIMARY KEY,
    department_id   BIGINT NOT NULL REFERENCES departments(id),
    college_id      BIGINT NOT NULL REFERENCES colleges(id),
    semester_number INT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    deleted_by      BIGINT
);
CREATE UNIQUE INDEX uq_semesters_dept_number
    ON semesters(department_id, semester_number) WHERE deleted_at IS NULL;
CREATE INDEX idx_semesters_department ON semesters(department_id);
CREATE INDEX idx_semesters_college ON semesters(college_id);

CREATE TABLE subjects (
    id              BIGSERIAL PRIMARY KEY,
    semester_id     BIGINT NOT NULL REFERENCES semesters(id),
    college_id      BIGINT NOT NULL REFERENCES colleges(id),
    subject_code    VARCHAR(20) NOT NULL,
    subject_name    VARCHAR(255) NOT NULL,
    max_marks       INT NOT NULL DEFAULT 100,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    deleted_by      BIGINT
);
CREATE UNIQUE INDEX uq_subjects_semester_code
    ON subjects(semester_id, subject_code) WHERE deleted_at IS NULL;
CREATE INDEX idx_subjects_semester ON subjects(semester_id);
CREATE INDEX idx_subjects_college ON subjects(college_id);
