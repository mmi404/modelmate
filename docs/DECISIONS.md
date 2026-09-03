# Architecture Decision Log

Short records of choices that shaped the build. Newest first.

## ADR-009 — One public domain, Caddy path routing
`/api/*` → Spring Boot, everything else → Next.js, on a single domain. Keeps
auth same-origin (cookie, no CORS), simplest TLS story, matches the shared-VPS
reverse-proxy pattern already used for the user's other projects.

## ADR-008 — Deploy: Docker Compose on VPS via GitHub Actions
No Kubernetes, no PaaS. CI builds and pushes images to GHCR; deploy job SSHes to
the VPS and runs `docker compose pull && up -d` + Flyway migrate. Rationale:
single small app, existing VPS, cost, matches the user's other deployments.

## ADR-007 — Cloudflare in front for edge caching + bot mitigation
Cloudflare proxy (orange cloud) for CDN, cache rules on static/public GET,
Bot Fight Mode, WAF managed rules, and Turnstile on auth forms. App-level
Bucket4j rate limiting is the second layer, not the only one.

## ADR-006 — SSR/ISR for all public pages
Model, category, leaderboard, and discussion pages are server-rendered with
`revalidate`, so crawlers and LLM agents receive complete HTML + JSON-LD.
Authed/interactive pages stay client-driven. Drives the SEO/GEO plan.

## ADR-005 — Reviews and problem reports share one table
`reviews.type = REVIEW | PROBLEM`. The proposal treats "post a review" and
"report a problem" as one flow; a problem is a review without ratings plus a
severity. Avoids a near-duplicate table and unifies voting/moderation.

## ADR-004 — Merge `model_submissions` into `models.status`
v1 had two structurally identical tables. One `models` table with
`PENDING | APPROVED | REJECTED` covers submission, moderation, and public
listing with a single filter.

## ADR-003 — Polymorphic votes via `(target_type, target_id)`
Replaces v1's three nullable FK columns + three unique keys with one row shape
and one unique constraint `(user_id, target_type, target_id)`.

## ADR-002 — PostgreSQL over MySQL
Better trigram/full-text search for model lookup, `citext`, richer indexing,
matches the user's tmscuet/reformcuet stacks. Flyway for schema; JPA
`ddl-auto=validate` everywhere.

## ADR-001 — Fresh rebuild, not a migration of version1.0
v1's frontend is Vite/React with hardcoded `localhost:8080` in ~17 files, the
backend has a 60 MB jar and `target/` committed, and large swaths are
uncommitted and untested. Cost of cleanup ≈ cost of rebuild, and the target
stack is different (Next.js). v1 is kept read-only as a feature/reference spec.

---

## Open decisions (need input before their phase)

| # | Question | Needed by |
|---|---|---|
| O-1 | GitHub repo: new `modelmate` repo, or overwrite existing `github.com/mmi404/modelmate`? | Phase 9 (CI/CD) |
| O-2 | Deploy target: the shared VPS `srv1385837`, or a different host? Domain name? | Phase 9 |
| O-3 | Production SMTP provider for password-reset email (Resend / Brevo / Mailgun / SES)? | Phase 2 or Phase 9 |
| O-4 | Use Cloudflare (assumed yes — used on other projects)? Account available for this domain? | Phase 7 |
| O-5 | Image uploads (model logos, avatars) — Cloudflare R2, or defer and use URL-only for v1? | Phase 5 |
