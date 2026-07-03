# Architecture Review — Pre-Sprint-1

Reviewed all 9 planning docs as a senior engineer about to inherit this codebase. Verdict up front: **the architecture is sound, the issues found were real but small, and they've been patched in place** (see diffs in each doc — nothing was redesigned). Details below.

---

## 1. Product
MVP scope still validates the actual business idea: does OCR + human verification produce a record people trust? Yes — nothing in scope is decorative.

**One addition, not a new feature but a corrected workflow:** the original docs described "Staff Verification → Rejected → Student Notified" but never specified what happens *after* rejection. That's not optional — a verification workflow with no path back from "rejected" isn't finished. Fixed: `PUT /marksheets/{id}/reupload`, ✅ Required.

## 2. Database
Found and fixed:
- `users.college_id` was `NOT NULL`, which is structurally impossible for SUPER_ADMIN (platform-level, not college-scoped). Now nullable with a CHECK constraint enforcing the right invariant per role. ✅ Fixed.
- `university_register_no` had no uniqueness constraint — a duplicate register number is exactly the kind of fraud/data-entry error this platform exists to catch, and the schema didn't catch it. ✅ Fixed.
- `virus_scan_status` was free-text `VARCHAR(20)` while every other status field is a proper enum — inconsistent, and lets invalid values in. ✅ Fixed.
- `ocr_results.raw_ocr_json` was `NOT NULL`, which breaks the moment OCR genuinely fails on a corrupt file or unreadable scan — a real scenario, not an edge case, given real-world marksheet photos. Added `status` + `failure_reason`, made the JSON nullable. ✅ Fixed.
- `password_reset_tokens` table was missing entirely — auth-flow.md referenced password reset as a maybe-later item; the database had nowhere to put it. ✅ Fixed (see below — reclassified as required, not optional).

No missing/unnecessary tables beyond that. Indexes were adequate for pilot scale; not adding more until real query patterns from the pilot justify them — speculative indexing is its own form of over-engineering.

## 3. ER Diagram
Cardinalities were correct. Multi-tenancy posture (soft — `college_id` present, no isolation logic) is still the right call for one pilot college; nothing to change here. Updated the SQL comments to make the reject/reupload relationship explicit so a future reader doesn't mistake the `UNIQUE(student_id, semester_id)` constraint for an oversight.

## 4. RBAC
This is where the most serious finding was:

**IDOR risk (privilege escalation / permission leak):** The original API contract said routes were "STAFF" or "STUDENT (owner)" scoped, but never stated *where* that ownership check lives. `@PreAuthorize("hasRole('STAFF')")` alone does not stop a staff member in Department A from approving/rejecting Department B's marksheets by guessing sequential IDs — role-based checks and ownership/scope checks are two different things, and only the first was specified. Same risk on the student side (student A reading student B's OCR review by ID). **Fixed:** added an explicit "Authorization" section to the API contract requiring service-layer ownership checks on every `{id}` route, independent of the role check. This was the single most important fix in this review — an unpatched IDOR on student academic records would be a genuine incident, not a bug ticket.

Also found: no path existed to create the first SUPER_ADMIN account (no self-registration by design, but also no bootstrap mechanism was documented — you'd have been locked out of your own platform on day one). Fixed with a Flyway seed migration, documented in the roadmap and API contract.

## 5. API Contract
- Missing endpoints that other docs already assumed existed: `/auth/verify-email`, `/auth/forgot-password`, `/auth/reset-password`, `/marksheets/{id}/reupload`. All added.
- No file upload constraints specified (size limit, MIME validation by magic bytes vs. trusting the extension). Added: 10MB cap, PDF-only validated by content, not filename.
- No pre-signed URL expiry specified. Added: 15 minutes.
- Versioning (`/api/v1`), pagination, error shape, and REST conventions were already solid — no changes needed there.

## 6. Authentication
- JWT + rotating refresh token strategy is still right — no session-table redundancy, appropriately simple for pilot scale.
- Password reset was left ambiguous ("add later, maybe"). Given real students will forget passwords within weeks of actual use, this isn't a deferrable nicety — reclassified ✅ Required and fully specified (see auth-flow.md).
- Rate limiting was only specified for login. Extended to `/auth/register` and `/auth/forgot-password` — both are open, unauthenticated endpoints and natural abuse targets (registration spam, email-bombing a student via password reset).
- CSRF: not applicable — this is a Bearer-token API with no cookie-based session, so CSRF's usual attack vector doesn't exist here. Worth stating explicitly so it's a documented decision, not a silent gap.

## 7. Folder Structure
No changes. Package-by-feature backend structure and the frontend's api/schemas/hooks split are appropriately sized for solo development — nothing to simplify or add.

## 8. Security
Beyond the IDOR and file-upload fixes above:
- Virus scanning had no named engine. Recommending self-hosted ClamAV as a Docker Compose sidecar — free, no per-scan API cost, appropriate for a bootstrapped budget.
- JWT secret and DB credentials: must come from environment variables / Docker secrets, never committed — flagging explicitly since it wasn't stated anywhere in the original docs and is a common first-project mistake.
- DPDP: full compliance program remains out of scope for a single-college pilot (per original risk assessment), but the concrete baseline — HTTPS, hashed passwords, private object storage, audit logs, now also file-content validation and rate limiting — is in place. Reasonable posture to defend if the college's admin asks.

## 9. Roadmap — can this actually ship in 3 weeks?
**Yes, with the Week 1 update.** The fixes above aren't new features bolted on top of the plan — they're corrections to things the plan already claimed to have (reupload was implied by the workflow diagram; password reset and email verification were referenced in auth-flow.md but not built). Surfacing them now, before Sprint 1, is cheaper than discovering them in Week 2 mid-build. Updated Week 1 in the roadmap to include: ClamAV in the Docker Compose stack, the Super Admin seed migration, and the full auth set (not just login/refresh) — all small, all belong in the foundation, none of it pushes the 3-week line.

---

## Final MVP Checklist
Everything required before launch — nothing else:

- [ ] Super Admin seed migration (bootstrap account)
- [ ] College + department + student seed data for pilot batch
- [ ] Auth: register → email verify → login → refresh → logout → forgot/reset password
- [ ] RBAC with service-layer ownership + department scope checks (not role-only)
- [ ] Marksheet upload: size/type validated, ClamAV scan, object storage, pre-signed URLs (15 min expiry)
- [ ] OCR pipeline with explicit PENDING/COMPLETED/FAILED states and a manual-entry fallback on failure
- [ ] Regex/rule-based validation + confidence score (no separate AI validation stage)
- [ ] Student review/correct UI → submit
- [ ] Staff pending queue → side-by-side compare → approve/reject with reason
- [ ] Reject → student notified → reupload → back to PENDING loop
- [ ] Student timeline view + verified record download
- [ ] Audit logs on every state-changing action
- [ ] Basic rate limiting on register/login/forgot-password/upload
- [ ] Deployed on a single VPS via Docker Compose

Explicitly NOT on this list (Phase 2+): staff search/filter/export, principal analytics, payments, marksheet version history, multi-tenant isolation.

## Development Order
```
Day 1        Seed migration (Super Admin) + Docker Compose (Postgres, ClamAV, app) + CI skeleton
Day 2-3      Auth module: register, verify-email, login, refresh, logout, forgot/reset password
Day 4-5      College/department/student CRUD (admin-created) + seed pilot batch data
             ↓
Week 1 gate: run 10-15 real marksheets through the OCR engine BEFORE building the
             pipeline around assumed formatting — this determines whether Week 2's
             plan needs adjusting
             ↓
Day 6-8      Upload endpoint (validation, ClamAV, storage) + OCR trigger + regex validation
Day 9-10     Student review/correct UI + submit + reupload-after-reject flow
             ↓
Day 11-13    Staff queue + compare view + approve/reject
Day 14       Student timeline + verified download + notifications
             ↓
Day 15-16    Audit logs wired through every module (retrofit is more expensive than building in) + rate limiting
Day 17-18    End-to-end testing with a real staff member (dry run, not just unit tests)
Day 19-21    Deploy, fix what breaks, pilot
```

## Risk Report — Top 10, ranked

| # | Risk | Probability | Impact | Mitigation |
|---|---|---|---|---|
| 1 | OCR accuracy worse than assumed on real marksheets | High | High | Test real samples Week 1 Day 5, before pipeline is built around assumptions |
| 2 | IDOR / broken access control (found in this review) | Was High pre-fix | Critical | Fixed at the design level; enforce in code review — every `{id}` route must show an explicit ownership check in the diff |
| 3 | Staff don't adopt verification workflow | Medium | High | Recruit one staff design partner before building; keep verification UI under 2 min/record |
| 4 | Timeline slips past 3 weeks | Medium-High | Medium | Pre-agreed cut order: staff search/filter/export first, then nothing else — core loop is non-negotiable |
| 5 | Password reset / email verification abused for spam | Medium | Medium | Rate limiting now specified on both endpoints |
| 6 | Solo-dev bus factor | High (structural) | Medium now, High if it scales | Acceptable for pilot; this doc set is the mitigation — revisit before institutional partnership |
| 7 | Regex validation too rigid, false-rejects valid marksheets | Medium | Medium | Validation produces warnings + confidence score, not hard blocks |
| 8 | Locked out of own platform (no Super Admin bootstrap) | Was High pre-fix | High | Fixed — Flyway seed migration on Day 1 |
| 9 | DPDP/data-protection concern raised by college admin | Medium | High (could kill approval) | Baseline security posture documented; be ready to explain it plainly, not defensively |
| 10 | College says no after demo | Medium | High (business, not technical) | Get informal staff buy-in before demo day, not cold |

## Architecture Readiness Score

| Dimension | Score | Note |
|---|---|---|
| Product | 8/10 | Scope is tight and validates the real question; reject/reupload loop was the one real gap, now closed |
| Architecture | 8/10 | Modular monolith remains the right call at this scale; no changes needed |
| Database | 8/10 | Was 6/10 before this review's fixes — nullability, uniqueness, and OCR-failure states were real gaps, now patched |
| Security | 7/10 | IDOR gap was the most serious finding of this whole review, now fixed at the design level; execution discipline in code review is what turns this into a 9 |
| Scalability | 7/10 | Appropriately un-scaled for one college; soft multi-tenancy in the schema means Phase 2 expansion isn't a rewrite |
| Maintainability | 8/10 | Package-by-feature + service-layer boundaries hold up |
| Developer Experience | 8/10 | Docs are concrete enough to start coding directly from them, which is the actual test |
| Documentation | 9/10 | Nine docs, no bloat, each one earns its place |

**Overall: 7.9/10** — solid, not perfect, appropriately so for a 3-week bootstrapped pilot.

## Go / No-Go Decision

**GO.** Architecture is frozen as of this review. The issues found (IDOR risk, missing reupload path, missing password reset, database nullability/uniqueness bugs) have been patched directly in the source docs — not deferred, not left as "known issues." Nothing found here requires a redesign; everything found required a small, specific fix, and those fixes are already in place.

Start Sprint 1 at Day 1 in the Development Order above. The one non-negotiable gate before Week 2 planning solidifies further: **run real marksheets through OCR by Day 5.** That result, not this document, is what actually tells you whether the 3-week line holds.
