# SEO, GEO & Abuse Prevention

> Implemented in **Phase 7**. This is the plan.

## SEO (search engines)

- **Rendering:** every public route is SSR or ISR (`revalidate: 3600` for model
  and category pages, `300` for leaderboard/community). Crawlers get full HTML.
- **Metadata:** Next.js `generateMetadata` per route — unique `<title>`,
  `description`, canonical URL. Model pages: `"<name> reviews & ratings — ModelMate"`.
- **Open Graph / Twitter cards:** per-page OG tags; dynamic OG images via
  `opengraph-image.tsx` (model name + avg rating + category).
- **Structured data (JSON-LD):**
  - `WebSite` + `SearchAction` on home
  - `SoftwareApplication` + `AggregateRating` on each model page
  - `Review` list items on model pages
  - `BreadcrumbList` on category/model pages
  - `Organization` sitewide
- **`sitemap.ts`:** dynamic — home, static pages, all categories, all approved
  models, public discussions. Regenerated on ISR revalidate.
- **`robots.ts`:** allow all public; disallow `/admin`, `/profile`, `/submit-*`,
  `/api`.
- **Semantics & a11y:** one `<h1>` per page, real heading hierarchy, `<article>`
  / `<nav>` / `<main>`, alt text, focus states — also helps rankings.
- **Performance (Core Web Vitals):** `next/image`, font `display: swap` +
  self-hosted Inter, route-level code splitting, no layout shift on cards,
  Lighthouse ≥ 95 target on model + home pages.
- **Internal linking:** category → model → related models; leaderboard → model.

## GEO (generative engines / AI agents)

- **`/llms.txt`** (and `/llms-full.txt`): plain-Markdown site summary + index of
  key URLs (categories, top models, API docs) for LLM crawlers.
- Keep the JSON-LD above — LLM crawlers consume it directly.
- Ensure server HTML contains the actual review text and ratings (not lazy-loaded
  client-only), so answer engines can cite ModelMate.
- Stable, readable URLs: `/models/gpt-4`, `/categories/computer-vision`.
- Public read API (`/api/v1/...`) documented and unauthenticated for GETs — an
  agent can pull structured data directly.
- `Last-Modified` / `ETag` on public API responses.

## Abuse prevention & bot mitigation

Layered:

1. **Cloudflare edge:** Bot Fight Mode, WAF managed ruleset, rate-limiting rule
   on `/api/v1/auth/*`, challenge on suspicious traffic, cache public GETs so
   scrapers hit the edge not the origin.
2. **Cloudflare Turnstile** on register, login (after N failures), forgot-password,
   model submission, new discussion. Verified server-side in `AuthService` /
   submission services behind a feature flag.
3. **App rate limiting (Bucket4j):**
   - auth endpoints: 5 / 15 min / IP
   - write endpoints (review, discussion, reply, model submit): 10 / hour / user
   - votes: 30 / min / user
   - search: 60 / min / IP
   Exceed → `429` with `Retry-After`.
4. **Account protection:** BCrypt (cost 12), generic auth errors (no user
   enumeration), password-reset code hashed + 15-min expiry + 5-attempt lock,
   lockout after 10 failed logins / 15 min.
5. **Input hardening:** Bean Validation on every DTO, length caps, JPA
   parameterised queries only, HTML-escape user content on render (React does
   this; sanitize any rich text), `websiteUrl` scheme allowlist (`http`/`https`).
6. **Headers:** HSTS, `X-Content-Type-Options: nosniff`, `Referrer-Policy:
   strict-origin-when-cross-origin`, `X-Frame-Options: DENY`, CSP (script-src
   self + explicit CDNs).
7. **Content moderation:** admin can hide reviews/discussions; `status` columns
   already model this. New-account write throttle (first 24 h: lower limits).
8. **Observability:** structured logs with request id + user id; count 4xx/429
   per IP; alert on spikes (simple log-based, or Cloudflare analytics).
