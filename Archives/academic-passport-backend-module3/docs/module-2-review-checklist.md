# Module 2 — Docker — Code Review Checklist

## What was verified in this sandbox
- [x] `docker-compose.yml` is syntactically valid YAML (parsed with PyYAML — Docker itself isn't installed in this sandbox, so `docker compose config` could not be run)
- [x] Dockerfile follows multi-stage build pattern (build stage discarded, only the JRE + jar ship in the final image)
- [x] Runtime container runs as non-root user (`app`, not root)
- [x] No secrets committed — `.env` is gitignored, `.env.example` has placeholders only, `.dockerignore` excludes `.env` from the build context too
- [x] `.dockerignore` present and excludes `target/`, `.git/`, `.env` — keeps the build context small and prevents secret leakage into image layers

## What could NOT be verified here (no Docker in this sandbox) — verify locally
- [ ] `docker compose config` — confirms full syntax + variable interpolation resolves correctly
- [ ] `docker build .` — confirms the Dockerfile actually builds (this also indirectly re-validates Module 1's `pom.xml`, since the build stage runs `mvn package`)
- [ ] `docker compose up --build` — confirms all three services actually start and pass their healthchecks
- [ ] Postgres tag `postgres:18-alpine` — confirm this tag exists and is the patch version you want (was `18.4` as of this module's research; Docker Hub tags may have moved on)
- [ ] ClamAV tag `clamav/clamav:stable` and the TCP-based healthcheck — the official image may ship a more thorough health-check script; worth checking docs.clamav.net/manual/Installing/Docker.html for their recommended healthcheck instead of the generic `nc` check used here
- [ ] Confirm your target VPS has enough RAM for ClamAV (docs recommend 2-4GB for clamd alone) alongside Postgres and the app JVM — this is a real constraint for a "very low budget" pilot deployment, not a formality

## Design decisions worth a second opinion
- **ClamAV runs as its own container from day one**, even though nothing calls it yet (upload/virus-scan integration is Sprint 2). Rationale: provisioning it now means Sprint 2 only has to write integration code, not also debug infrastructure under a tighter deadline. If you'd rather defer the ClamAV container until it's actually used, that's a reasonable alternative — flag it and I'll pull it out of this module.
- **Postgres exposes port 5432 to the host** (not just the Docker network) for local development convenience (connecting via a DB client). Fine for a pilot; if you want to lock this down before any real student data is loaded, remove the `ports:` mapping on `postgres` and rely on the Docker network only — the `app` service doesn't need the host port mapping to reach it.
