# Module 3 — Database Architecture Proposal (PostgreSQL + Flyway + JPA)

This is a design document, not code. It builds directly on the reviewed schema from
`02-database-schema.sql` (already frozen in architecture review) — nothing here changes
that schema's shape. What's new: how it gets expressed as versioned Flyway migrations,
how JPA maps onto it, and two decisions that weren't fully settled before (Super Admin
seeding mechanism, ddl-auto policy) that need sign-off before I write a single migration.

---

## 1. Entity Inventory

| Entity | Table | Owns identity of |
|---|---|---|
| College | `colleges` | A pilot institution |
| Department | `departments` | Org unit within a college |
| User | `users` | Login identity (student/staff/super admin) |
| RefreshToken | `refresh_tokens` | Active refresh sessions |
| PasswordResetToken | `password_reset_tokens` | Active reset requests |
| Student | `students` | Student profile, 1:1 with User |
| Staff | `staff` | Staff profile, 1:1 with User |
| Semester | `semesters` | A semester within a department |
| Subject | `subjects` | A subject within a semester |
| Marksheet | `marksheets` | One student's upload for one semester |
| OcrResult | `ocr_results` | OCR output for a marksheet, 1:1 |
| MarksheetSubject | `marksheet_subjects` | Per-subject extracted marks |
| Verification | `verifications` | Staff approve/reject decision, 1:1 with Marksheet |
| AuditLog | `audit_logs` | Append-only action log |
| Notification | `notifications` | In-app notification |
| SupportTicket | `support_tickets` | Support request |

16 entities. Nothing added or removed from the frozen schema — this is purely an inventory for mapping to JPA classes 1:1 by table.

## 2. Relationships & Cardinality

```
College 1───N Department 1───N Student
College 1───N User(SUPER_ADMIN excluded)   Department 1───N Staff
                                            Department 1───N Semester 1───N Subject
User 1───1 Student (nullable — only for STUDENT role)
User 1───1 Staff    (nullable — only for STAFF role)
User 1───N RefreshToken
User 1───N PasswordResetToken
User 1───N Notification
User 1───N SupportTicket
User 1───N AuditLog

Student 1───N Marksheet (one per semester, enforced by UNIQUE(student_id, semester_id))
Marksheet 1───1 OcrResult
Marksheet 1───N MarksheetSubject
Marksheet 1───1 Verification
Subject 1───N MarksheetSubject
Staff 1───N Verification
Semester 1───N Marksheet
```

No cardinality changes from the frozen ERD — restated here because the JPA mapping decisions below (section 5) depend on getting `@OneToOne` vs `@OneToMany` right per relationship, and that's worth confirming against this table before generating annotated entities.

## 3. Indexes (as already defined, restated for this review)

| Table | Index | Purpose |
|---|---|---|
| `students` | `idx_students_department` | Staff queue queries filter by department |
| `marksheets` | `idx_marksheets_student` | Student timeline lookups |
| `verifications` | `idx_verifications_status` | Staff pending-queue query (`WHERE status = 'PENDING'`) |
| `audit_logs` | `idx_audit_logs_user` | Per-user activity lookups (admin) |
| `refresh_tokens` | `idx_refresh_tokens_user` | Token validation/revocation lookups |
| `users` | `uq_users_college_email` (partial unique) | Per-college email uniqueness, excludes SUPER_ADMIN |
| `users` | `uq_users_super_admin_email` (partial unique) | Global email uniqueness for SUPER_ADMIN only |
| `students` | `UNIQUE(department_id, roll_number)` | No duplicate roll numbers within a department |
| `students` | `UNIQUE(university_register_no)` | Global uniqueness (fixed in architecture review) |
| `marksheets` | `UNIQUE(student_id, semester_id)` | One marksheet slot per student per semester (reject→reupload reuses the row) |

No new indexes proposed. Deliberately not adding speculative indexes (e.g. on `marksheet_subjects.subject_id` beyond the FK) until the pilot's real query patterns justify them — same anti-over-engineering stance as the rest of this project.

## 4. Constraints Beyond Indexes
- `CHECK` on `users`: SUPER_ADMIN must have `college_id IS NULL`, every other role must have it `NOT NULL` — already specified, restating because it directly affects how the JPA entity's validation annotations should be written (can't rely on a single `@NotNull` on `collegeId`; needs a class-level or service-level check mirroring this).
- All foreign keys use the default `NO ACTION` on delete except where `ON DELETE CASCADE` is explicit (`refresh_tokens`, `password_reset_tokens`, `students`, `staff`, `marksheet_subjects`, `ocr_results`, `verifications` — all cascade from their parent `users` or `marksheets` row). Rationale: deleting a user should clean up their sessions; deleting a marksheet (shouldn't normally happen, but if an admin needs to) should clean up its OCR/verification children. Colleges/departments/students are NOT cascade-deletable from their parents — deleting a college should be a deliberate, blocked-by-default operation, not something that silently cascades through every student record.

## 5. JPA Mapping Conventions

- **ID generation:** `GenerationType.IDENTITY` — matches `BIGSERIAL`, simplest option, no need for a sequence-based strategy at this scale.
- **Enums:** mapped with `@Enumerated(EnumType.STRING)` against the Postgres native enum types (`user_role`, `verification_status`, `ticket_status`, `scan_status`, `ocr_status`). Storing as STRING, not ORDINAL — protects against silent data corruption if an enum's declared order ever changes.
- **JSONB columns** (`ocr_results.raw_ocr_json`, `validation_errors`, `audit_logs.metadata`): mapped via Hibernate's native JSON support (`@JdbcTypeCode(SqlTypes.JSON)` on a `String` or a Jackson-backed `JsonNode` field) — no separate library needed for Hibernate 7.
- **Auditing fields** (`created_at`, `updated_at`): `@CreatedDate`/`@LastModifiedDate` via Spring Data JPA auditing (`@EnableJpaAuditing`), not manually set in service code — removes an entire class of "forgot to set updated_at" bugs.
- **Entities never leave the service layer** (per the folder-structure doc's coding standard) — every entity gets a corresponding DTO; this module only builds entities + repositories, not DTOs or controllers (those come with each feature module in Modules 4+).
- **No bidirectional `@OneToMany` mappings unless a real use case needs traversal from the "one" side.** E.g. `Department` won't carry a `List<Student> students` field just because the relationship exists — `StudentRepository.findByDepartmentId(...)` covers every real query. Bidirectional mappings are a common source of N+1 query bugs and unintentional cascade behavior; only adding them where the access pattern actually requires it.

## 6. Flyway Migration Strategy

**Versioning:** sequential `V{n}__description.sql`, one migration per logical domain grouping (not one-giant-file, not one-file-per-table). Grouping by domain keeps related tables/constraints/indexes atomic within a single reviewable migration, while keeping the blast radius of any future fix small — a bug found in the marksheet migration doesn't require re-touching the college/department migration.

| Migration | Contents |
|---|---|
| `V1__extensions_and_enums.sql` | Postgres enum types: `user_role`, `verification_status`, `ticket_status`, `scan_status`, `ocr_status` |
| `V2__colleges_and_departments.sql` | `colleges`, `departments` |
| `V3__users_and_tokens.sql` | `users` (+ CHECK constraint, partial unique indexes), `refresh_tokens`, `password_reset_tokens` |
| `V4__students_and_staff.sql` | `students`, `staff` |
| `V5__academic_structure.sql` | `semesters`, `subjects` |
| `V6__marksheets_and_verification.sql` | `marksheets`, `ocr_results`, `marksheet_subjects`, `verifications` |
| `V7__support_and_audit.sql` | `audit_logs`, `notifications`, `support_tickets` |

Each file includes its own indexes and constraints inline with its table definitions (not deferred to a later "add indexes" migration) — an index that's missing between migration N and N+3 is a real (if brief) production gap during deployment; no reason to introduce it deliberately.

**What's explicitly NOT a Flyway migration:**
- **Super Admin seeding is NOT a migration.** Hardcoding a bootstrap password hash into a SQL file that lives in git is a real credential-hygiene problem — anyone with repo access can see the hash and attempt to crack it offline, and rotating it means writing a new migration rather than just changing a config value. Instead: a `CommandLineRunner` (or `ApplicationRunner`) bean, gated by `spring.profiles.active` or an explicit `app.bootstrap.enabled` flag, that reads `SUPER_ADMIN_EMAIL` / `SUPER_ADMIN_PASSWORD` from environment variables at startup and creates the account **only if zero SUPER_ADMIN users exist yet** (idempotent — safe to leave enabled, it's a no-op after the first successful boot). This is Module 4's actual implementation; flagging the mechanism now because it changes what Module 3 needs to NOT do (no seed data migration).
- **No repeatable migrations (`R__...`)** for this MVP — no reference/lookup data changes often enough to justify them at pilot scale. Revisit if that changes.

**Flyway configuration (`application.yml`, added this module):**
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: false   # fail loudly on a non-empty, non-Flyway-managed DB rather than silently baselining
    validate-on-migrate: true    # checksum mismatch = fail startup, not silent drift
    out-of-order: false          # migrations must apply in strict sequence
  jpa:
    hibernate:
      ddl-auto: validate         # Flyway owns the schema. Hibernate is only allowed to
                                  # VALIDATE its entity mappings match reality, never to
                                  # create/update DDL itself. This is the single most
                                  # important line in this config — "update" here would
                                  # silently undo the point of having Flyway at all.
    open-in-view: false          # avoid the lazy-loading-in-controller anti-pattern
```

**Rollback posture:** Flyway Community (which is what a bootstrapped budget uses — the paid Teams tier adds undo migrations) has no automatic rollback. Forward-only migrations are the plan: a bad migration gets fixed by writing a new migration that corrects it, not by reverting history. This is standard practice, not a gap, but worth stating explicitly since it affects how carefully each migration needs to be reviewed before it ships — there's no undo button once it's run against a real database with real student data in it.

## 7. Testing Strategy for This Module

**Testcontainers, not H2.** The schema uses Postgres-specific features (native enums, `JSONB`, partial unique indexes) that H2's Postgres-compatibility mode doesn't faithfully emulate — a green test against H2 wouldn't actually prove the migrations work against real Postgres. Tests spin up `postgres:18-alpine` via Testcontainers, run the real Flyway migrations against it, then run repository-level tests against that real instance.

**Carrying forward the same honesty from prior modules:** Testcontainers requires Docker, and this sandbox doesn't have Docker installed (confirmed in Module 2). So I can write these tests, but — same as `mvn test` in Module 1 and `docker compose up` in Module 2 — I cannot execute them here to prove green. You'll run `mvn test` locally where Docker is available and Testcontainers can actually pull and start the Postgres image.

Planned test coverage for this module once implementation starts:
- Flyway migration test: all 7 migrations apply cleanly in sequence against a fresh container
- Repository tests per entity: basic CRUD + the specific constraint-violation cases that matter (duplicate `university_register_no` rejected, duplicate `(student_id, semester_id)` marksheet rejected, SUPER_ADMIN with a non-null `college_id` rejected by the CHECK constraint)

## 8. Repository Layer (interfaces only, one per aggregate root)
`CollegeRepository`, `DepartmentRepository`, `UserRepository`, `RefreshTokenRepository`, `PasswordResetTokenRepository`, `StudentRepository`, `StaffRepository`, `SemesterRepository`, `SubjectRepository`, `MarksheetRepository`, `OcrResultRepository`, `MarksheetSubjectRepository`, `VerificationRepository`, `AuditLogRepository`, `NotificationRepository`, `SupportTicketRepository` — all `extends JpaRepository<Entity, Long>`, package-by-feature per the folder structure doc (e.g. `StudentRepository` lives in `student/`, not a shared `repository/` package).

---

## Open Decisions Needing Your Sign-Off Before I Write Code

1. **Super Admin bootstrap via `CommandLineRunner` + env vars, not a Flyway seed migration.** (Section 6.) This is a real change from what might have been assumed — flagging it clearly since it affects Module 4.
2. **`ddl-auto: validate`, never `update`.** Standard practice, but confirming explicitly since getting this wrong is the single most common way a team ends up with Flyway and Hibernate fighting over schema ownership.
3. **Migration grouping by domain (7 files)** vs. one-file-per-table (16 files) vs. one giant file. Proposing 7 as the balance between reviewability and not over-fragmenting — open to a different split if you disagree.
4. **No repeatable migrations, no seed/reference data migrations for MVP.**

If these look right, next step is generating the actual `V1`–`V7` migration SQL files, the 16 JPA entity classes, and the 16 repository interfaces — still no service/controller logic yet, that arrives with Modules 4+.
