# Architecture Decision Log

Short records of choices that shaped the build. Newest first.

## ADR-012 — Public browse pages are NOT gated behind login
`ModelMate_Updated_UI_Design_Instructions.pdf` says "Not logged in: Always
redirected to Login Page" — reasonable for a coursework static-site mockup,
but incompatible with this build's explicit SEO/GEO requirement (crawlers and
LLM agents can't index a page that redirects them to a login wall). Decision:
**home, categories, category detail, model detail, compare, leaderboard, and
community/discussion reading are public.** Only actions that write data
(submit review/problem, submit model, start a discussion/reply, profile,
admin) require auth, enforced in `proxy.ts`. The two-tier nav from the design
doc is kept, repurposed: **Navbar** carries primary navigation for everyone
(Home, Categories, Compare, Leaderboard, Community) plus auth state; **Sidebar**
appears only when logged in, for the personal/action items (Post a Review,
Submit a Model, Profile, Admin if applicable).

## ADR-011 — Next.js 16 landed; stick to the "previous" caching model, use `proxy.ts`
`create-next-app@latest` resolved to **Next.js 16.3.4 / React 19.2.8** (newer
than the 15.x assumed in `ARCHITECTURE.md`; that doc's version number is now
stale, behavior described still applies). Two Next 16 changes affect this build:

- `middleware.ts` is deprecated and renamed **`proxy.ts`** (function `proxy`,
  not `middleware`). Node.js runtime by default. We use `proxy.ts` from the start.
- Next 16 ships an opt-in **Cache Components** model (`cacheComponents: true`
  in `next.config.ts`) requiring `"use cache"` directives and `<Suspense>`
  around every dynamic/runtime read. We do **not** enable it — too large a
  paradigm shift to take on alongside everything else in this build. We use
  the "previous model" instead: `fetch(url, { next: { revalidate } })` and
  route-segment `export const revalidate = N`, exactly as planned in
  `ARCHITECTURE.md`. This is still fully supported, just not the new default
  path forward. Revisit once the app is stable.

Per the framework's own `AGENTS.md` (auto-generated into `frontend/`): consult
`frontend/node_modules/next/dist/docs/` before writing App Router code that
looks routine — conventions have moved since training data.

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

## ADR-010 — Deployment specifics (resolved 2026-09-04)

- **Repo:** overwrite the existing `github.com/mmi404/modelmate` — this v2 tree
  replaces its history. Push with a fresh initial commit; the old `modelmate.git`
  content is preserved only in `version1.0/.git` locally.
- **Host:** the shared VPS `srv1385837`, registering with its existing shared
  Caddy (per ADR-009).
- **Domain:** `modelmate.mmi404.com` (Cloudflare-proxied).
- **Object storage:** Cloudflare R2 for model logos and user avatars (S3 API,
  same Cloudflare account, zero egress) — built in Phase 5.
- **Production email:** deferred. Password-reset flow is built and works against
  MailHog in dev; prod runs with mail disabled (the endpoint returns 204 and
  logs the code server-side) until an SMTP/API provider is chosen. Tracked in
  the parking lot.

---

## Open decisions

*All resolved — see ADR-010. Production email provider is deferred (parking lot).*
