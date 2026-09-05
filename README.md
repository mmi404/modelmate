# ModelMate

Community-driven platform to **review, rate, compare, and discuss AI models**.
Users browse models by category, read and post multi-criteria reviews and problem
reports, compare models side by side, follow a leaderboard of top-rated models,
and take part in community discussions.

This is a **fresh rebuild** (v2). The previous iteration lives at
`../version1.0` (React + Vite + Spring Boot + MySQL) and is kept for reference
only — nothing here depends on it.

## Stack

| Layer     | Choice |
|-----------|--------|
| Frontend  | Next.js 16 (App Router) · TypeScript · Tailwind · shadcn/ui · TanStack Query |
| Backend   | Spring Boot 3.4 · Java 21 · Spring Security + JWT · Spring Data JPA · Flyway · springdoc-openapi |
| Database  | PostgreSQL 16 |
| Infra     | Docker Compose · Caddy reverse proxy · GitHub Actions CI/CD · deployed to VPS |

## Repository layout

```
modelmate/
├── frontend/            Next.js app
├── backend/             Spring Boot app
├── infra/               Caddy config, deploy scripts, compose overrides
├── docs/                Architecture, data model, API spec, decisions, deployment
├── docker-compose.yml   Local dev: postgres + adminer + mailhog
├── .env.example
└── TASKS.md             Master task list — the source of truth for progress
```

## Local development

```bash
cp .env.example .env
cp frontend/.env.example frontend/.env.local
docker compose up -d            # postgres (:5433), adminer (:8081), mailhog (:8025)

# backend (:8080) — reads DB_URL / JWT_SECRET from the repo-root .env
cd backend && set -a && . ../.env && set +a && ./mvnw spring-boot:run

# frontend (:3000) — reads frontend/.env.local
cd frontend && pnpm install && pnpm dev
```

Password-reset emails are caught by Mailhog at <http://localhost:8025>.
Seed admin (dev only): `admin@modelmate.local` / `admin12345`.

Checks: `cd backend && ./mvnw verify` · `cd frontend && pnpm lint && pnpm typecheck && pnpm build`

## Production

Images are built by CI and deployed to the VPS via `docker-compose.prod.yml`.
See [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md). To run the full prod stack locally:

```bash
cp .env.prod.example .env         # set POSTGRES_PASSWORD, JWT_SECRET, SITE_URL
docker compose -f docker-compose.prod.yml up --build   # add --profile edge for bundled Caddy
```

## Documentation

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — system design, auth flow, module map
- [`docs/DATA-MODEL.md`](docs/DATA-MODEL.md) — entities, relationships, schema
- [`docs/API.md`](docs/API.md) — REST endpoint reference
- [`docs/DECISIONS.md`](docs/DECISIONS.md) — architecture decision log
- [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) — VPS + CI/CD runbook
- [`docs/SEO-GEO.md`](docs/SEO-GEO.md) — SEO, GEO, and abuse-prevention plan
