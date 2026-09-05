# ModelMate v2 — Master Task List

Source of truth for progress. Check items off as they land. Each **▣ commit**
marker is a real git commit point — we commit at every stage, not at the end.

Legend: `[ ]` todo · `[~]` in progress · `[x]` done · `[!]` blocked (needs input)

---

## Phase 0 — Planning & repo setup  ← *current*

- [x] Read `version1.0` (entities, controllers, schema, seed) + design PDFs + proposal
- [x] Decide stack, DB, scope, repo layout (see `docs/DECISIONS.md`)
- [x] Monorepo skeleton, `.gitignore`, `README.md`
- [x] `docs/ARCHITECTURE.md`
- [x] `docs/DATA-MODEL.md`
- [x] `docs/API.md`
- [x] `docs/DECISIONS.md`, `docs/DEPLOYMENT.md`, `docs/SEO-GEO.md`
- [x] `TASKS.md` (this file)
- [x] `docker-compose.yml` (postgres + adminer + mailhog) + `.env.example`
- [x] **▣ commit:** `chore: scaffold monorepo, architecture + API docs, task list`
- [x] Resolve open decisions → ADR-010 (overwrite `mmi404/modelmate`, shared VPS
      `srv1385837`, domain `modelmate.mmi404.com`, Cloudflare R2 storage, prod
      email deferred)

## Phase 1 — Backend foundation

- [x] Spring Boot 3.4.2 project (Java 21): web, validation, data-jpa, security,
      flyway, postgresql, springdoc-openapi, jjwt, lombok; Testcontainers for tests
- [x] Maven wrapper, `.mvn`, package structure per `ARCHITECTURE.md §3`
- [x] `application.yml` + `application-dev.yml` + `application-prod.yml` +
      test, all config env-driven, `ddl-auto: validate`
- [x] `common/`: `ApiError`, `GlobalExceptionHandler`, `PageResponse<T>`,
      `SlugUtil`, `RequestIdFilter`, typed exceptions (Hibernate `@CreationTimestamp`
      used per-entity instead of a `BaseEntity` superclass — cleaner schema match)
- [x] `OpenApiConfig`, `CorsConfig` (env origins), `SecurityConfig` (stateless,
      permit-all placeholder), `/actuator/health` + `/api/v1/ping`
- [x] Flyway `V1__initial_schema.sql` — full schema from `docs/DATA-MODEL.md`
      (checks, partial unique index, pg_trgm)
- [x] Entities + repositories (proper `@ManyToOne`, `Short` for `smallint` cols)
- [x] `V2__seed_reference_data.sql` — 12 categories + 12 approved models + system
      user; dev admin seeded by `DevDataInitializer` (`admin@modelmate.local`)
- [x] Boots clean against Docker Postgres; Flyway 2/2; Hibernate validate passes;
      Swagger renders; health green; Testcontainers context test green
- [x] **▣ commit:** `feat(backend): project skeleton, schema, entities, seed`

  Dev note: local Postgres on host port **5433** (5432 held by another project).

## Phase 2 — Backend auth & security

- [x] `JwtService` (access token + reset ticket, configurable TTL), `JwtAuthFilter`
      (bearer header or `mm_session` cookie), `@AuthenticationPrincipal AuthUser`
- [x] `SecurityConfig`: stateless, public GET matchers, `/api/v1/auth/**` public
      except `/me` `/logout`, `/admin/**` = ADMIN, BCrypt(12); JSON 401/403 handlers
- [x] `AuthController` + `AuthService`: register, login, me, logout
- [x] Password reset: forgot → BCrypt-hashed 6-digit code + email (MailHog),
      verify-code → 10-min reset ticket, reset-password; 15-min expiry, 5-attempt cap,
      no user enumeration; event-driven `MailService`
- [x] `MailService` (`@EventListener` on `PasswordResetCodeIssued`, text template,
      no-op + log when `modelmate.mail.enabled=false`)
- [x] `RateLimitingFilter` (Bucket4j, 5 / 15 min / IP on auth endpoints, `429` +
      `Retry-After`, in security chain)
- [x] `CaptchaService` — Turnstile siteverify behind `security.captcha.enabled`
      (default off; live path untested — needs Cloudflare)
- [x] `ModelMateProperties` (`@ConfigurationProperties`); `GlobalExceptionHandler`
      extended (404 / 405 / 400 / 409 framework mappings)
- [x] Tests (12 green): register/login/me, dup email 409, validation 400,
      full reset flow, wrong code, no-enumeration, rate-limit 429, role 403/401.
      Testcontainers switched to JVM-singleton pattern (shared cached context).
- [x] **▣ commit:** `feat(backend): JWT auth, password reset, rate limiting`

## Phase 3 — Backend domain APIs

- [x] **Categories:** list (+computed modelCount group query), detail, models-in-category
- [x] **Models:** list/filter/sort(newest|name|rating|reviews)/paginate (native
      query w/ aggregate join), detail (5-dim aggregate ratings + problem count),
      trending, search typeahead, names, compare (2-3), submit (→PENDING, unique slug)
- [x] **Reviews/Problems:** list reviews, list problems, create (REVIEW needs all
      5 ratings + one-per-user; PROBLEM has severity), edit (author), delete
      (author/admin, orphan votes cleaned); feed model aggregate
- [x] **Leaderboard:** ranked native query, category filter + minReviews, top-3 flag
- [x] **Discussions:** list (tag filter, sort newest|active|top), detail, create,
      tags-with-counts, stats
- [x] **Replies:** list (one level of threading, deeper nesting flattened), create
      (bumps replyCount)
- [x] **Votes:** PUT upsert / DELETE, polymorphic `Votable`, transactional counters
- [x] **Users/Profile:** public profile (no email), contributions (merged
      reviews+problems+discussions+replies), PUT /me, /me/contributions (incl. hidden)
- [x] **Admin:** pending list, approve, reject (reason), hide/unhide review, stats
- [x] Manual DTOs + records; every write endpoint `@Valid`; `PageResponse<T>` wrapper
- [x] Integration tests: catalog(11), review(6), community(6), admin/profile(6),
      auth(10), rate-limit(1), context(1) = **41 green** (Testcontainers singleton,
      per-test reset to seed baseline)
- [x] **▣ commit** per module (`categories & models`, `reviews … leaderboard`,
      `discussions, replies, votes`, `admin & profile`)

**Backend Phase 1-3 complete: 36 endpoints live under `/api/v1`, Swagger at
`/api/v1/docs`.** Remaining backend work (Bucket4j on write endpoints, `Last-Modified`
/ ETag, structured logging) folded into Phase 6-7.

## Phase 4 — Frontend foundation

- [x] Next.js **16.3.4** (React 19.2.8) App Router + TS + Tailwind v4 + ESLint;
      pnpm; `next dev --webpack` (Turbopack OOM'd on this machine's free RAM —
      see TASKS note below; webpack is fully supported, just not the new default)
- [x] shadcn/ui init (radix base, Nova preset) — button, input, label, avatar,
      dropdown-menu, sheet, separator, sonner, skeleton, card, badge, tooltip,
      textarea, select. version1.0's components weren't portable (older shadcn/
      Tailwind 3 API); rebuilt fresh against the current CLI instead.
- [x] Design tokens (dark theme from PDF: `#0D0D0D`/`#1A1A1A`/`#4F46E5`/etc.) as
      CSS custom properties in `globals.css`; self-hosted Inter via `next/font/google`
- [x] App shell: `Navbar` (70px, search input, primary nav, auth-aware avatar
      menu), `Sidebar` (250px, **authed only** — see ADR-012), `Footer` (60px);
      mobile: hamburger → `Sheet` drawer with full nav
- [x] Typed API layer: `lib/api/backend-fetch.ts` (server-only, attaches
      session as Bearer) + `app/api/backend/[...path]/route.ts` (same-origin
      proxy so Client Components never see the backend URL or need CORS) +
      `lib/api/client.ts` (browser `apiFetch`); `lib/api/types.ts` mirrors the
      backend DTOs
- [x] `proxy.ts` (middleware→proxy rename, ADR-011) gates write/personal routes
      only (ADR-012); `AuthProvider` (server-derived context, no client fetch);
      `/api/login`, `/api/register`, `/api/logout` route handlers set/clear the
      httpOnly `mm_session` cookie; `getCurrentUser()` server util (`React.cache`)
- [x] TanStack Query provider, `sonner` Toaster (dark), `TooltipProvider`
- [x] `frontend/.env.example` + `.env.local`
- [x] Verified end-to-end against the live backend: home page SSR-renders all
      12 categories from Postgres; register sets the cookie; authed home page
      swaps Login/Register for the avatar menu; `proxy.ts` redirects an
      anonymous visit to `/submit-review` → `/login?next=...`; `tsc --noEmit`
      and `eslint` both clean
- [x] **▣ commit:** `feat(frontend): Next.js shell, design system, auth plumbing`

  Dev note: this machine runs low on free RAM (~1GB) with Docker + IDE + backend
  all up — `pnpm dlx`/`shadcn add` OOM'd in large batches (fixed by adding
  components a few at a time) and Turbopack's Rust process OOM'd outright
  (fixed by running dev with `--webpack`, set as the `dev` script default).
  Stop Docker containers and the Java backend when not actively testing.

## Phase 5 — Frontend pages

- [x] **Auth:** `/login`, `/register` (two-column per PDF), `/forgot-password` (3 steps)
- [ ] **Home `/`:** trending carousel, latest reviews list, popular categories grid
- [x] **Categories `/categories`** + **`/categories/[slug]`** (vertical model cards, mini-ratings)
- [x] **Model detail `/models/[slug]`:** overview, 5-criteria rating bars, reviews
      (comment style), problems (accordion, by severity), vote buttons
- [x] **Post review/problem `/submit-review`:** model typeahead, type toggle,
      star inputs, severity, textarea
- [x] **Compare `/compare`:** 2–3 model pickers, side-by-side rating table
- [ ] **Leaderboard `/leaderboard`:** ranked table, category filter, top-3 highlight
- [x] **Community `/community`:** discussion list, tag filter, stats sidebar, tag cloud
- [x] **Discussion detail `/community/[id]`:** thread, replies, nested reply, voting
- [x] **New discussion `/community/new`**
- [x] **Submit model `/submit-model`**
- [ ] **Profile `/profile` + `/users/[id]`:** info + contributions list
- [ ] **Admin `/admin`:** pending submissions, approve/reject, hide reviews, stats
- [ ] Loading / empty / error states for every page; `not-found.tsx`, `error.tsx`
- [ ] **▣ commit** per page group

## Phase 6 — Integration, polish, hardening

- [ ] Every page wired to the real API; zero mock data
- [ ] Full click-through of each user journey against running backend
- [ ] Responsive QA at 360 / 768 / 1024 / 1440
- [ ] Accessibility pass (keyboard, focus, contrast, aria, screen-reader smoke)
- [ ] Security headers (Next `headers()` + CSP), cookie flags, URL scheme allowlist,
      secrets audit, dependency audit (`pnpm audit`, `mvn dependency-check`)
- [ ] Perf: `next/image`, code-split, caching headers, Lighthouse ≥ 90 all pages
- [ ] Consistent error toasts, form validation messages, disabled/pending buttons
- [ ] **▣ commit:** `chore: integration polish, a11y, security headers, perf`

## Phase 7 — SEO / GEO / bot protection

- [ ] `generateMetadata` per route; canonical URLs; OG + Twitter tags
- [ ] Dynamic `opengraph-image` for model pages
- [ ] JSON-LD: WebSite, SoftwareApplication+AggregateRating, Review, BreadcrumbList, Organization
- [ ] `sitemap.ts` (dynamic), `robots.ts`, `/llms.txt` + `/llms-full.txt`
- [ ] Confirm SSR/ISR revalidate values; verify review text is in server HTML
- [ ] Bucket4j limits tuned per `SEO-GEO.md`; `429` + `Retry-After`
- [ ] Turnstile wired on register/login/forgot/submit/new-discussion
- [ ] Cloudflare config notes: proxy, cache rules, Bot Fight, WAF, rate-limit rule
- [ ] `Last-Modified`/`ETag` on public GET API responses
- [ ] **▣ commit:** `feat: SEO metadata, JSON-LD, sitemap, GEO, rate-limit tuning`

## Phase 8 — Dockerize

- [ ] `backend/Dockerfile` (multi-stage, JRE 21), `.dockerignore`
- [ ] `frontend/Dockerfile` (multi-stage, `output: standalone`), `.dockerignore`
- [ ] `docker-compose.prod.yml` (frontend, backend, postgres, caddy) + healthchecks
- [ ] `infra/Caddyfile` (or shared-proxy site block) — path routing + TLS + headers
- [ ] Full stack runs locally via prod compose; migrations apply; smoke test green
- [ ] **▣ commit:** `build: production Dockerfiles, compose, Caddy config`

## Phase 9 — CI/CD & first deploy

- [ ] Force-push v2 tree to `github.com/mmi404/modelmate` (replaces old history)
- [ ] `.github/workflows/ci.yml` — lint/typecheck/test/build, path-filtered
- [ ] `.github/workflows/deploy.yml` — build+push GHCR, SSH deploy, migrate, health-gate
- [ ] `infra/bootstrap.sh` — VPS `srv1385837` first-time setup, add site block to shared Caddy
- [ ] GitHub secrets configured; `/opt/modelmate/.env` on VPS (user supplies SSH key + R2 creds)
- [ ] Cloudflare: `modelmate.mmi404.com` DNS → VPS, proxied; TLS via shared Caddy
- [ ] First deploy; smoke test all public + one authed flow in prod
- [ ] Nightly `pg_dump` cron + prune
- [ ] **▣ commit:** `ci: CI + deploy pipelines, VPS bootstrap`

## Phase 10 — Post-deploy

- [ ] Uptime check (Cloudflare / UptimeRobot); error-rate glance
- [ ] `docs/DEPLOYMENT.md` runbook finalised with real values
- [ ] `README` quickstart verified from clean clone
- [ ] Short handover / "how to operate" note
- [ ] **▣ commit:** `docs: finalise runbook and handover`

---

## Parking lot (post-v1, from the proposal's "Future Recommendation")

- AI-generated review summaries per model
- Smart recommendations from user preferences
- "Model Battle" — user-voted head-to-head
- New-model alerts / notifications + email digest
- **Production email provider** (Resend / Brevo / SES) — wire real SMTP for
  password-reset; until then prod runs mail-disabled (code logged server-side)
- Mobile app
