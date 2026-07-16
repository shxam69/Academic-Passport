-- V4: Students and staff.
--
-- `college_id` is denormalized here even though it's derivable via department_id ->
-- departments.college_id. This is the concrete mechanism behind "tenant-aware from
-- the beginning": every repository method that scopes by tenant can filter
-- `WHERE college_id = ?` directly with a single index, instead of every query needing
-- a join through departments. It also sets up trivially for Row-Level Security
-- policies in Phase 2 (`USING (college_id = current_setting(...))`), which need exactly
-- this shape — a direct college_id column on the table being protected.
-- Tradeoff: college_id must be kept consistent with department_id at write time.
-- This is safe in practice because a student/staff member's department (and therefore
-- college) is not expected to change after creation in this MVP — there's no
-- "transfer department" feature — so there's no update path where the two could drift.

CREATE TABLE students (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    department_id           BIGINT NOT NULL REFERENCES departments(id),
    college_id              BIGINT NOT NULL REFERENCES colleges(id),
    full_name               VARCHAR(255) NOT NULL,
    roll_number             VARCHAR(50) NOT NULL,
    university_register_no  VARCHAR(50) NOT NULL,
    dob                     DATE NOT NULL,
    section                 VARCHAR(10),
    batch_year              INT NOT NULL,
    current_semester        INT NOT NULL DEFAULT 1,
    shift                   VARCHAR(10),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    version                 BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_students_dept_roll
    ON students(department_id, roll_number) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_students_register_no
    ON students(university_register_no) WHERE deleted_at IS NULL;
CREATE INDEX idx_students_department ON students(department_id);
CREATE INDEX idx_students_college ON students(college_id);

CREATE TABLE staff (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    department_id   BIGINT NOT NULL REFERENCES departments(id),
    college_id      BIGINT NOT NULL REFERENCES colleges(id),
    full_name       VARCHAR(255) NOT NULL,
    designation     VARCHAR(100),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    deleted_by      BIGINT,
    version         BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_staff_department ON staff(department_id);
CREATE INDEX idx_staff_college ON staff(college_id);
