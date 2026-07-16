# Module 3 — PostgreSQL + Flyway + JPA — Code Review Checklist

## What was verified in this sandbox
- [x] All 7 Flyway migrations pass `sqlfluff lint --dialect postgres` with zero syntax errors (style-only warnings: line length, `CREATE INDEX CONCURRENTLY` suggestion — the latter doesn't apply here since these run against empty tables at initial migration time, and Flyway wraps migrations in a transaction by default, where `CONCURRENTLY` isn't even permitted)
- [x] `pom.xml` is well-formed XML (caught and fixed one malformed attribute I introduced mid-edit — see commit history)
- [x] Every entity's package matches its directory; every declared type matches its filename; every file's braces balance (scripted sweep across all 16 entities + 16 repositories)
- [x] No missing imports for `List`, `Optional`, `BigDecimal`, `Instant`, `LocalDate`, `Page`, `Pageable` across all files (scripted sweep; caught and fixed one real miss — `SupportTicketRepository` was missing `import java.util.List`)
- [x] Cross-checked every entity's base-class choice against the actual migration columns before writing entities, not after — this caught a real design bug: my original Module 3 proposal assumed every entity has both `created_at`/`updated_at`, but 6 of 16 tables don't (by deliberate design). Fixed the base-class hierarchy (`IdentifiedEntity` → `BaseEntity`/`CreatedOnlyEntity`; `VersionedEntity` now extends `IdentifiedEntity`, not `BaseEntity`) before it became 16 broken entities and a startup-time `ddl-auto=validate` failure

## What could NOT be verified here (no Docker in this sandbox) — verify locally
- [ ] `mvn test` — the two test classes written (`CollegeRepositoryTest`, `StudentRepositoryTest`) exercise real behavior against Testcontainers Postgres 17, but have not actually been run
- [ ] Full Flyway migration apply against a real Postgres 17 instance (the context-load test in `AcademicPassportApplicationTests` is designed to prove this, but again — not executed here)
- [ ] `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` and `org.hibernate.annotations.SQLRestriction` — both are established Hibernate 6.2+/6.3+ APIs I'm reasonably confident carry forward unchanged into Hibernate 7 (Spring Boot 4.1's Hibernate version), but I can't confirm against the real Hibernate 7 Javadoc from here since Boot 4.1 postdates my reliable knowledge. If `mvn compile` fails on either annotation, check Hibernate 7's migration notes for a renamed equivalent.
- [ ] `flyway-database-postgresql` artifact version resolution via the Spring Boot 4.1.0 BOM — noted inline in `pom.xml`

## Test coverage scope — a deliberate choice, not an oversight
Only 2 of 16 entities have dedicated repository tests so far (`College` — soft-delete
+ partial-unique-index reuse; `Student` — tenant-scoping enforcement + optimistic
locking). This is intentional: those two tests prove the *patterns* (soft delete,
tenant scoping, versioning) work correctly at the schema/ORM level, and every other
soft-deletable/versioned/tenant-scoped entity follows the identical pattern. Writing
14 more near-identical tests now, before any service/controller code exists to
actually exercise these repositories, would be testing the pattern a second (through
sixteenth) time rather than testing new behavior. Each feature module (4+) will add
tests for its own repository as it's actually wired into real request flows — that's
where entity-specific edge cases (e.g. the marksheet reject→reupload interaction
with the partial unique index) are more meaningfully tested anyway, against real
service-layer logic rather than raw repository calls in isolation.

## Design decisions worth a second opinion
- **`deleted_by` has no FK constraint** on any table (soft reference to `users.id`, validated at the application layer only). Necessary given migration ordering (colleges/departments precede users), but worth knowing this is a spot where the DB itself won't catch a bad value — only the service layer will.
- **College's own `deleted_at` doesn't cascade to its departments/students/etc.** Soft-deleting a college does NOT automatically soft-delete everything under it — those rows remain independently visible unless separately deleted. If you want "delete a college" to cascade the soft-delete down the tree, that's a service-layer orchestration to add explicitly (a transaction that soft-deletes the college, then all its departments, then all their students, etc.) — not something the schema does for you. Flagging because the previous (hard-delete) schema's `ON DELETE CASCADE` behavior doesn't carry over to soft delete automatically, and it would be easy to assume it does.
- **Audit logs are the one place tenant scoping is optional, not enforced** — `AuditLogRepository.findAllByCollegeId` exists, but nothing stops a query for `findAll()` (inherited from `JpaRepository`) from returning every college's logs at once. That's deliberate (SUPER_ADMIN is explicitly platform-wide per the RBAC matrix), but worth confirming that's still the intended behavior now that tenant-awareness is a stated priority.
