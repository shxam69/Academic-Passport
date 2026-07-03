# Academic Passport — Backend

Modular monolith, Spring Boot 4.1.0 / Java 21. See `/mnt/user-data/outputs` planning docs
(01-PRD through 10-architecture-review) for the frozen architecture this implements.

## Sprint 1 status
- [x] Module 1 — Project scaffold
- [x] Module 2 — Docker
- [ ] Module 3 — PostgreSQL + Flyway
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

## Testing
```bash
mvn test
```
**Note on this repo's build environment:** these files were authored in a sandbox without
access to Maven Central, so `mvn test` has not been executed by the tool that wrote this
code — only reviewed for correctness. Run it yourself before merging each module; treat
that as the actual gate, not this note.

## Requirements
- Java 21
- Maven 3.9+ (3.8.7 also confirmed compatible)
