# Risk Assessment — Pilot Phase

Scoped to what can actually kill *this* pilot, not a generic enterprise risk register.

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| **OCR accuracy on real marksheets is worse than expected** — inconsistent scan quality, phone photos, varied formatting across semesters | High | High | Student review-and-correct step is mandatory before submit — OCR doesn't need to be perfect, it needs to save time over manual entry. Test against 10-15 *real* sample marksheets from your college in week 1, before building the full pipeline around assumed formatting. |
| **Staff don't adopt the verification workflow** — seen as extra unpaid work | Medium | High | Keep staff-side UI to under 2 minutes per verification (side-by-side compare, one-click approve). Recruit one friendly staff member as a design partner before building, not after. |
| **Regex validation is too rigid**, rejects valid marksheets due to format edge cases | Medium | Medium | Validation produces warnings + confidence score, not hard blocks — student/staff can override with a flag, not stuck in a dead end. |
| **Solo-dev bus factor** — no one else can maintain this if you're unavailable | High (structural) | Medium during pilot, High if it scales | Acceptable risk at pilot stage. Document as you build (this doc set is step one). Revisit before any institutional partnership — colleges will ask "what if you disappear." |
| **DPDP / data protection concerns raised by college admin** | Medium | High (could kill the pilot approval) | You're handling student PII and academic records — non-negotiable minimum: HTTPS, hashed passwords, private object storage with pre-signed URLs, audit logs. Full DPDP compliance program is not required for a single-college pilot, but be ready to explain your security posture in plain language when you pitch. |
| **Scope creep from this very document set** — you, mid-build, deciding to add "just one more feature" | High | High | This is the one you control directly. Every feature request gets checked against the PRD's MVP scope section. If it's not there, it's Phase 2. |
| **Timeline slips past 3 weeks** | Medium-High | Medium | Pre-agreed cut list (per your earlier answer): staff search/filter/export goes first. If more needs to go, cut principal-facing anything (already deferred) before touching the core upload→OCR→verify loop — that loop *is* the product. |
| **College says no after the demo** | Medium | High (business risk, not technical) | Mitigate by recruiting one supportive staff member and getting informal buy-in *before* demo day, not cold-pitching a finished product to a principal with no internal advocate. |
