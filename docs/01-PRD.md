# Academic Passport — Product Requirements (Lean PRD)

## Mission
Give every student in the pilot college one permanent, verifiable academic record — replacing scattered PDFs in WhatsApp/Drive/email with a single trusted timeline that staff verify and (eventually) recruiters can trust.

## What the MVP Must Prove
One thing, end to end: **a student can upload a marksheet, the system extracts the data, staff verify it, and a trusted record exists afterward.**
If a feature doesn't serve that loop, it is out of scope — full stop, regardless of what's listed elsewhere in this doc set.

## Target Users (MVP)
- Student
- Staff (verifier)
- Super Admin (you, operating the platform)

Principal is **explicitly deferred** — see Non-Goals. You don't need principal-level analytics to prove the core loop; you need one department trusting the verification workflow.

## Pilot Scope
- 1 college (yours)
- 1 department
- 1 batch
- No institutional partnership yet — this is a demo to win one

## MVP Feature Scope (IN)

**Student**
- Register (college code required)
- Login
- Upload semester marksheet (PDF)
- Review OCR-extracted data, correct if needed
- Submit for verification
- View academic history / timeline
- Download verified record (PDF)

**Staff**
- Login
- View pending verification queue
- Open uploaded PDF side-by-side with OCR output
- Edit OCR mistakes
- Approve / Reject (with reason)

**Super Admin**
- Register the pilot college
- Create staff accounts
- View basic platform logs (who uploaded what, who verified what)

**Stretch (cut first if week-2 crunch hits, per your call):**
- Staff search/filter/export of student records

## Non-Goals (explicitly NOT in MVP — Phase 2/3)
- Payments / subscriptions
- Principal analytics & department reports
- Recruiter/employer verification portal
- LinkedIn / Naukri integration
- Career recommendations, AI assistant
- Public APIs
- Mobile apps
- Multi-OCR-engine fallback
- Full multi-tenant infrastructure (tenant isolation, per-tenant scoping) — schema is *tenant-ready* (college_id FK everywhere relevant) but no isolation logic is built until college #2 signs on
- Separate AI-validation stage on top of OCR — regex/rule-based validation only for MVP (per your call)

## Core Workflow (the thing being proven)
```
Student uploads PDF
  → Virus scan
  → PDF → image → OCR
  → Regex/rule-based validation + confidence score
  → Student reviews & corrects
  → Student submits
  → Staff opens pending queue, compares OCR vs source PDF
  → Staff approves or rejects
  → If approved: record locked, added to student's verified timeline
  → Student notified, can download verified PDF
```

## Success Metrics for the Pilot (define "done")
- ≥ 90% of uploaded marksheets successfully OCR'd with reviewable output (doesn't need to be 100% accurate — student review step catches errors)
- Staff can verify a submission in under 2 minutes
- Zero data leaks across student accounts (basic security bar, not full DPDP audit)
- At least one full batch (department, one semester's worth) processed end-to-end
- Principal/college admin says "yes, expand this to the rest of the college" after the demo

## Constraints
- Solo founder, solo developer
- Very low budget — bootstrap, prefer free/open-source tiers (self-hosted Tesseract or free-tier cloud OCR, not paid enterprise OCR)
- 3-week timeline to MVP
- No institutional backing yet — everything must work with zero IT support from the college

## Key Assumptions (flag if wrong)
- Marksheets follow a semi-consistent format within your college (varies by university, not per-student) — OCR/regex rules are built against your college's actual marksheet layout, not a generic one
- Students have smartphones/laptops and can produce a reasonably clean PDF or photo
- Staff are willing to do verification as an added task without separate training/incentive during pilot
- College will tolerate a non-official, founder-run pilot without needing a formal contract first

## Out-of-band workstreams (run in parallel, NOT gating code)
Business model, pricing strategy, competitor analysis, investor readiness — these matter for the startup, but nothing about the schema, API, or auth flow depends on them. Do these alongside Phase 1 build or after the pilot validates demand, not before.
