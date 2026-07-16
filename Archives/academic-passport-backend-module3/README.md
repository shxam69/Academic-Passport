# Academic Passport — Backend

Modular monolith, Spring Boot 4.1.0 / Java 21. See `/mnt/user-data/outputs` planning docs
(01-PRD through 10-architecture-review) for the frozen architecture this implements.

## Sprint 1 status
- [x] Module 1 — Project scaffold
- [x] Module 2 — Docker
- [x] Module 3 — PostgreSQL + Flyway + JPA
- [ ] Module 4 — Super Admin bootstrap
- [ ] Module 5 — Authentication
- [ ] Module 6 — Email verification
- [ ] Module 7 — Password reset
- [ ] Module 8 — Student registration
- [ ] Module 9 — Student login

## Running locally (without Docker)
```bash
mvn spring-boot:run
```
App starts on `:8080`. No database dependency yet at this module — that lands in Module 3.

## Running with Docker
```bash
cp .env.example .env      # then fill in real local values
docker compose up --build
```
Starts the app, Postgres, and ClamAV together. Postgres and ClamAV are provisioned now
but not yet wired into the app itself — that's Module 3 (DB) and the upload module in
Sprint 2's OCR pipeline work. This module is about proving the infrastructure boots
cleanly, not about the app using it yet.

**Note:** ClamAV's first boot downloads its full signature database, which takes a
few minutes — the `start_period: 120s` in the healthcheck accounts for this, but don't
be alarmed if `clamav` shows `starting` for a while on first run.

## Database
7 Flyway migrations under `src/main/resources/db/migration`, applied automatically
on startup against `postgres:17-alpine` (via `docker compose up`, or your own local
Postgres 17 instance with `POSTGRES_DB`/`POSTGRES_USER`/`POSTGRES_PASSWORD` matching
`.env`). `ddl-auto: validate` — Flyway owns the schema, Hibernate only checks its
entity mappings against it. If you see a validate-mismatch error on startup, that's
Hibernate telling you an entity's annotations don't match what the migrations
actually created — fix the entity or write a new migration, never flip this to
`update`.

See `docs/module-3-database-architecture.md` for the full design rationale
(soft-delete scope, tenant-scoping strategy, optimistic locking scope, migration
grouping).

## Testing
```bash
mvn test
```
Repository/integration tests extend `AbstractIntegrationTest`, which spins up a
real `postgres:17-alpine` container via Testcontainers and runs the actual Flyway
migrations against it — not H2, since the schema leans on Postgres-specific JSONB,
native enums, and partial unique indexes that H2's compatibility mode doesn't
faithfully emulate. **Requires Docker.** This sandbox does not have Docker
installed (confirmed in Module 2), so these tests have been reviewed for
correctness but not executed by the tool that wrote them — run `mvn test` locally
or in CI to actually get a green/red result.

## Requirements
- Java 21
- Maven 3.9+ (3.8.7 also confirmed compatible)
