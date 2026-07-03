# Authentication Flow — MVP

Single session strategy: **JWT access token (short-lived, 15 min) + rotating refresh token (hashed at rest, 7-day expiry)**. No separate sessions table — see schema notes for why.

## Registration (Student)
```
Student → POST /auth/register { collegeCode, name, rollNumber, universityRegisterNo,
                                  dob, department, section, year, semester, shift,
                                  email, mobile, password }
Server  → validate collegeCode exists & is_active
        → validate uniqueness (college_id + email, department_id + rollNumber)
        → hash password (BCrypt)
        → create users row (role=STUDENT, is_verified=false)
        → create students row
        → send verification email/OTP
        → 201 Created
Student → verifies email/OTP → is_verified=true
```

## Login
```
User → POST /auth/login { email, password }
Server → look up user by (college_id resolved from email domain OR collegeCode, email)
       → verify password hash
       → check is_active AND is_verified
       → issue access_token (JWT, 15 min, claims: userId, role, collegeId)
       → issue refresh_token (random 256-bit, hashed with SHA-256, stored in refresh_tokens)
       → return both to client
```

## Authenticated Request
```
Client → any /api/v1/* route → Authorization: Bearer <access_token>
Server → JWT filter validates signature + expiry
       → SecurityContext populated with userId/role/collegeId
       → @PreAuthorize on controller enforces role (see RBAC matrix)
       → service layer enforces row-level ownership (student can only touch own records)
```

## Refresh
```
Client → POST /auth/refresh { refreshToken }
Server → hash incoming token, look up in refresh_tokens
       → check not revoked, not expired
       → revoke old token (rotation — one-time use)
       → issue new access_token + new refresh_token
```

## Logout
```
Client → POST /auth/logout { refreshToken }
Server → mark matching refresh_tokens row revoked=true
```

## Password Reset (promoted to required — see review notes)
```
User   → POST /auth/forgot-password { email }
Server → if user exists: generate token, hash it, store in password_reset_tokens (30 min expiry)
       → send reset link via email (token in URL, not the raw token stored anywhere)
       → ALWAYS return 200 regardless of whether the email exists (don't leak account existence)
User   → clicks link → POST /auth/reset-password { token, newPassword }
Server → hash incoming token, look up unexpired unused row
       → update password_hash, mark token used=true
       → revoke ALL existing refresh_tokens for that user (force re-login everywhere)
```

## Failure handling
- 3 failed logins within 15 min → 429 + temporary lockout (basic rate limiting, not a full fraud system — MVP-appropriate)
- Expired/revoked refresh token → 401, client forces re-login
- `/auth/register`, `/auth/forgot-password` are also rate-limited per IP — both are free-form public endpoints and otherwise open to abuse (registration spam, email-bombing a student via forgot-password)
