# Development Roadmap

## Phase 0 — Planning (done, this doc set)
Lean PRD, DB schema + ERD, RBAC matrix, API contract, auth flow, folder structure/standards, risk assessment. ~1 day, not 1-2 weeks — the point of trimming 25 docs to 8 was to get to code fast.

## Phase 1 — MVP Build (3 weeks, solo)

**Week 1**
- Project scaffold: Spring Boot modules + React/Vite frontend, Docker Compose (Postgres + app + ClamAV), CI skeleton
- Flyway seed migration for the first SUPER_ADMIN account (no self-registration path — see API contract)
- Test OCR against 10-15 real marksheets from your college FIRST, before building the pipeline around assumptions — this is the highest-risk unknown, de-risk it early
- Auth: register, email verification, login, refresh, logout, forgot/reset password, JWT filter, RBAC guards with explicit ownership/department checks (not role-only)
- College/department/student seed data for the pilot batch

**Week 2**
- Marksheet upload (virus scan → object storage → OCR trigger)
- OCR pipeline: PDF→image, OCR call, regex validation, confidence score
- Student review/correct UI, submit flow
- **Crunch checkpoint:** if behind schedule, cut staff search/filter/export now (pre-agreed) — do not touch the upload→OCR→verify loop

**Week 3**
- Staff verification queue, side-by-side compare, approve/reject
- Student timeline + verified record download (pre-signed URL)
- Super admin: college registration, staff creation, audit log view
- Deploy (single Docker Compose stack on a cheap VPS is enough for a pilot — do not over-provision infra for one department)
- Dry run with real staff member before the actual demo

## Phase 2 — Post-Pilot (only after college says yes)
- Payments/subscriptions
- Principal analytics & department reports
- Notifications (beyond basic in-app)
- Multi-tenant isolation logic (only now does college #2 justify it)
- Formal DPDP compliance review

## Phase 3 — Scale (only after Phase 2 is stable in production)
- Recruiter/employer verification portal
- LinkedIn/Naukri integration
- Career recommendations, AI assistant
- Mobile apps
- Public APIs

## Guardrail for every phase
Before adding anything not listed above: **does this help validate the MVP's core loop, or win the next stage of adoption?** If neither, it's a distraction — no matter how good the idea is on its own.
