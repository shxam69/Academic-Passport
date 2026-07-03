# RBAC Matrix — MVP Scope

Roles: **STUDENT**, **STAFF**, **SUPER_ADMIN** (Principal role deferred to Phase 2 — see PRD Non-Goals).

| Capability | Student | Staff | Super Admin |
|---|:---:|:---:|:---:|
| Register (with college code) | ✅ | ❌ (admin creates staff accounts) | ❌ (bootstrapped manually) |
| Login / Refresh token | ✅ | ✅ | ✅ |
| Upload marksheet PDF | ✅ (own record only) | ❌ | ❌ |
| View own OCR review & edit before submit | ✅ | ❌ | ❌ |
| Submit marksheet for verification | ✅ | ❌ | ❌ |
| View own academic history/timeline | ✅ | ❌ | ❌ |
| Download own verified record | ✅ | ❌ | ❌ |
| Raise support ticket | ✅ | ✅ | — (receives/handles, doesn't raise) |
| Re-upload after rejection (own record only) | ✅ | ❌ | ❌ |
| View pending verification queue | ❌ | ✅ (own department only) | ✅ (all) |
| Open PDF + compare to OCR | ❌ | ✅ (own department only) | ✅ |
| Edit OCR-extracted values | ❌ | ✅ (own department only) | ❌ (not their job) |
| Approve / reject marksheet | ❌ | ✅ (own department only) | ❌ |
| Search/filter/export students *(stretch — cut first under crunch)* | ❌ | ✅ | ✅ |
| Register a college | ❌ | ❌ | ✅ |
| Create staff accounts | ❌ | ❌ | ✅ |
| View audit logs | ❌ | ❌ (own actions only, via own history) | ✅ (all) |
| Handle support tickets | ❌ | ❌ | ✅ |

## Enforcement notes
- Role is a single enum on `users.role` — no separate permissions table for MVP (see schema notes).
- Department scoping for STAFF is enforced at the query layer: `WHERE department_id = :staffDepartmentId`, not by a generic permission system. One rule, applied consistently, beats a flexible-but-unused permission engine at this stage.
- SUPER_ADMIN bypasses department scoping but every write action still goes through `audit_logs`.
- A student can only ever read/write rows where `marksheets.student_id` resolves to their own `students.id` — enforce this in the service layer, not just at the controller/route level, so a direct repository call can't accidentally skip the check. Same applies to staff and department scoping — role check alone (`@PreAuthorize("hasRole('STAFF')")`) is not sufficient and creates an IDOR hole if ownership/department isn't separately verified. See API Contract's "Authorization" section for the concrete rule.
- SUPER_ADMIN has no self-registration path — first account is seeded at deploy time, not created through the API. See API Contract's "Super Admin bootstrap" note.
