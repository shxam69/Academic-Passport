# Folder Structure & Coding Standards

## Backend — Spring Boot Modular Monolith
Package-by-feature, not package-by-layer. Each module owns its controller/service/repository/dto — this keeps the "modular" in modular monolith and makes future extraction to a service (if ever needed) far less painful than a package-by-layer structure would.

```
src/main/java/com/academicpassport/
├── config/              # Security, CORS, Swagger, ObjectStorage, exception handling
├── auth/                 # register, login, refresh, JWT filter
│   ├── controller/
│   ├── service/
│   ├── dto/
│   └── entity/           # RefreshToken
├── college/               # colleges, departments
├── student/                # students, timeline
├── staff/                   # staff, verification queue
├── marksheet/                 # upload, ocr trigger, review, submit
│   ├── ocr/                    # OCR client, regex validators, confidence scoring
├── verification/
├── admin/                        # super admin: college registration, audit logs
├── notification/
├── support/
├── common/                          # shared DTOs, pagination, error response shape
└── AcademicPassportApplication.java
```

**Rules:**
- No module reaches into another module's `entity`/`repository` directly — go through its `service` interface. This is what keeps a monolith modular instead of a ball of mud.
- Every entity has a corresponding DTO; entities never leave the service layer (no leaking JPA entities into controllers).
- Global exception handler (`@ControllerAdvice`) — one error shape, no ad-hoc try/catch scattered across controllers.
- Flyway for migrations, not `ddl-auto: update`. Migration files versioned in `src/main/resources/db/migration`.

## Frontend — React + TypeScript + Vite

```
src/
├── api/                  # one file per module: authApi.ts, marksheetApi.ts, etc — TanStack Query hooks live here
├── components/
│   ├── ui/                 # generic, reusable (Button, Input, Table)
│   └── feature/              # feature-specific (MarksheetUploader, OcrReviewTable)
├── pages/                       # route-level components
│   ├── student/
│   ├── staff/
│   └── admin/
├── routes/                          # React Router config, role-based route guards
├── schemas/                           # Zod schemas, shared between form validation and API types
├── hooks/
├── store/                                # minimal — auth state only; TanStack Query owns server state
├── types/
└── lib/                                     # axios instance, utils
```

**Rules:**
- Zod schema is the single source of truth for a form's shape — infer TS types from it (`z.infer<typeof schema>`), don't hand-write parallel interfaces.
- Server state lives in TanStack Query, not in global state (Zustand/Redux) — don't duplicate what the cache already gives you.
- No inline API calls in components — always through `api/*.ts`.

## Naming & Commits
- REST paths: plural nouns, kebab-case (`/support-tickets`, not `/supportTicket`).
- DB: `snake_case`. Java: `camelCase`/`PascalCase`. TS: `camelCase`/`PascalCase`.
- Commits: Conventional Commits (`feat:`, `fix:`, `chore:`, `refactor:`) — cheap to adopt solo, pays off the moment you bring on a second engineer or need to write release notes for the college.
- One feature branch per module slice (`feat/marksheet-upload`), merge to `main` via PR even solo — forces you to re-read your own diff before it ships, which catches real bugs.
