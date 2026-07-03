# Academic Passport — Backend

Modular monolith, Spring Boot 4.1.0 / Java 21. See `/mnt/user-data/outputs` planning docs
(01-PRD through 10-architecture-review) for the frozen architecture this implements.

## Sprint 1 status
- [x] Module 1 — Project scaffold
- [ ] Module 2 — Docker
- [ ] Module 3 — PostgreSQL + Flyway
- [ ] Module 4 — Super Admin bootstrap
- [ ] Module 5 — Authentication
- [ ] Module 6 — Email verification
- [ ] Module 7 — Password reset
- [ ] Module 8 — Student registration
- [ ] Module 9 — Student login

## Running locally
```bash
mvn spring-boot:run
```
App starts on `:8080`. No database dependency yet at this module — that lands in Module 3.

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
