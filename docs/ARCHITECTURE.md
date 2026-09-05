# ModelMate — Architecture

## 1. Overview

ModelMate is a two-tier web application:

```
                    ┌──────────────────────────────────────────────┐
   Browser  ───────▶│  Next.js 16 (App Router, SSR/ISR)   :3000    │
   / AI agent       │  - public pages server-rendered for SEO/GEO  │
                    │  - authed pages behind middleware            │
                    │  - talks to backend over HTTPS, same domain  │
                    └───────────────┬──────────────────────────────┘
                                    │  /api/v1/**  (proxied by Caddy)
                    ┌───────────────▼──────────────────────────────┐
                    │  Spring Boot 3.4 REST API           :8080    │
                    │  - stateless JWT auth                        │
                    │  - Bean-validated DTOs                       │
                    │  - Bucket4j rate limiting                    │
                    │  - Flyway-managed schema                     │
                    └───────────────┬──────────────────────────────┘
                    ┌───────────────▼──────────────────────────────┐
                    │  PostgreSQL 16                       :5432    │
                    └──────────────────────────────────────────────┘
```

Single public domain. Caddy terminates TLS and routes:
- `/api/*` → Spring Boot
- everything else → Next.js

## 2. Frontend

- **Next.js App Router.** Public routes (`/`, `/categories`, `/models/[slug]`,
  `/leaderboard`, `/compare`, `/community`, discussion pages) are
  server-rendered / ISR so crawlers and LLM agents get full HTML.
- **Authed routes** (`/submit-model`, `/submit-review`, `/profile`,
  `/community/new`, `/admin`) are gated by `proxy.ts` (Next.js 16 renamed
  `middleware.ts` → `proxy.ts`, ADR-011) which checks for a session cookie and
  redirects to `/login`. Everything else is public — ADR-012.
- **Auth model:** backend issues a JWT; Next.js stores it in an **httpOnly,
  Secure, SameSite=Lax cookie** set via a route handler (`/api/session`). The
  browser never sees the token. Server Components read the cookie and call the
  backend with it; Client Components call the backend through a thin fetch
  wrapper that always sends credentials.
- **Data fetching:** Server Components for first paint; TanStack Query in Client
  Components for interactive lists, mutations, optimistic voting.
- **UI:** Tailwind v4 + shadcn/ui (rebuilt fresh against the current CLI —
  version1.0's components predate Tailwind v4 and weren't portable as-is).
  Design tokens from `ModelMate_Updated_UI_Design_Instructions.pdf`:
  - bg `#0D0D0D`, surface `#1A1A1A`, accent `#4F46E5`, text `#F9FAFB` / `#9CA3AF`,
    border `#2C2C2C`; font Inter; 8px radius; card shadow `0 2px 8px rgba(0,0,0,.3)`.
  - Layout: top Navbar (70px) + left Sidebar (250px, authed only) + Footer (60px).

## 3. Backend

Package root `com.modelmate`.

```
config/         SecurityConfig, CorsConfig, OpenApiConfig, RateLimitConfig, MailConfig
security/       JwtService, JwtAuthFilter, CurrentUser resolver
auth/           AuthController, AuthService, password-reset flow
user/           User entity, UserController (profile, contributions)
category/       Category entity + controller/service/repo
model/          Model entity, ModelController, ModelService, moderation
review/         Review entity (REVIEW | PROBLEM), controller/service/repo
discussion/     Discussion, Reply, DiscussionController, ReplyController
vote/           Vote entity, VoteController (polymorphic target)
leaderboard/    read-model queries
common/         ApiError, GlobalExceptionHandler, PageResponse<T>, BaseEntity, slug util
```

- **Stateless.** No server session. `JwtAuthFilter` validates the bearer token
  (read from the `Authorization` header the Next.js server attaches, or from the
  cookie for direct calls) and sets the `SecurityContext`.
- **Roles:** `USER`, `ADMIN`. `/api/v1/admin/**` requires `ADMIN`.
- **Validation:** every write endpoint takes a `@Valid` DTO; failures return a
  `400` with field errors in the standard `ApiError` shape.
- **Errors:** `GlobalExceptionHandler` maps exceptions to
  `{ timestamp, status, error, message, path, fieldErrors? }`.
- **Migrations:** Flyway. `V1__init.sql` … versioned; no `ddl-auto` in any
  environment (`validate` only).
- **Docs:** springdoc-openapi serves `/api/v1/openapi.json` and Swagger UI at
  `/api/v1/docs` (dev + staging only).
- **Rate limiting:** Bucket4j in-memory buckets keyed by IP for auth endpoints
  (register/login/forgot-password) and by user id for write endpoints.

## 4. Auth flow

```
Register / Login
  Browser → POST /login (Next route handler)
         → Next server → POST /api/v1/auth/login  {email,password}
         ← { token, user }
  Next route handler sets httpOnly cookie `mm_session=<jwt>`, returns { user }

Authenticated request
  Server Component  → backendFetch() reads the cookie, calls Spring Boot directly
  Client Component  → apiFetch() → same-origin /api/backend/[...path] route handler
                       (reads the cookie server-side, forwards as Bearer, proxies
                       the response back) → Spring Boot
  Spring validates the JWT, resolves AuthUser

Logout
  POST /logout → Next clears cookie

Password reset
  forgot-password → 6-digit code emailed (MailHog in dev, SMTP in prod),
  stored hashed with 15-min expiry + attempt_count cap
  verify-reset-code → short-lived reset ticket
  reset-password → sets new hash, invalidates token
```

## 5. Environments

| | dev | prod |
|---|---|---|
| Frontend | `pnpm dev` :3000 | Docker, `output: standalone`, behind Caddy |
| Backend  | `mvnw spring-boot:run` :8080, profile `dev` | Docker, profile `prod` |
| DB       | Docker Postgres :5432 | Docker Postgres, named volume + nightly `pg_dump` |
| Mail     | MailHog :8025 | SMTP (provider TBD) |
| Secrets  | `.env` (gitignored) | `/opt/modelmate/.env` on VPS, injected by compose |
| Swagger  | on | off |

## 6. Cross-cutting

- **CORS:** dev allows `http://localhost:3000`; prod is same-origin so CORS is
  effectively closed (Caddy proxies `/api`).
- **Security headers:** set at Caddy (HSTS, X-Content-Type-Options, Referrer-Policy,
  CSP) + Next.js `headers()` for app-specific CSP.
- **Logging:** JSON logs to stdout, collected by Docker; request-id filter.
- **IDs & slugs:** numeric PKs internally; `categories` and `models` also carry a
  unique `slug` used in URLs.
- **Time:** all timestamps `timestamptz`, UTC.
