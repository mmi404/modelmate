# Deployment & CI/CD

> Detailed steps are filled in during **Phase 8–9**. This is the target design.

## Topology

```
Cloudflare (DNS + proxy + WAF + Turnstile + edge cache)
        │  HTTPS
        ▼
VPS  ── Caddy ──┬── :3000  frontend  (Docker, Next.js standalone)
   (Docker)     ├── :8080  backend   (Docker, Spring Boot, profile=prod)
                └── :5432  postgres  (Docker, named volume)
                    + nightly pg_dump → /opt/modelmate/backups
```

All containers on one Docker network; only Caddy publishes 80/443.
If the target is the shared VPS, ModelMate registers with the **existing shared
Caddy** instead of running its own (see ADR-009 / open decision O-2).

## Images

- `ghcr.io/<owner>/modelmate-frontend:<sha>` and `:latest`
- `ghcr.io/<owner>/modelmate-backend:<sha>` and `:latest`
- Multi-stage builds; frontend uses `output: 'standalone'`; backend uses a JRE-21
  runtime layer over the Maven build layer.

## GitHub Actions

**`ci.yml`** (on PR + push):
1. `frontend`: `pnpm install`, `pnpm lint`, `pnpm typecheck`, `pnpm test`, `pnpm build`
2. `backend`: `./mvnw verify` (unit + Testcontainers integration), Flyway validate
3. path filters so a frontend-only PR skips the backend job and vice versa

**`deploy.yml`** (on push to `main`, after CI passes):
1. build + push both images to GHCR, tagged with the commit SHA
2. SSH to VPS → `cd /opt/modelmate` → `docker compose pull`
3. run backend one-shot `flyway migrate` (or let the app do it on boot with a lock)
4. `docker compose up -d` → wait for `/actuator/health` + frontend `200`
5. rollback: re-deploy previous SHA tag on health-check failure

## Secrets (GitHub → repo settings → Secrets)

| name | purpose |
|---|---|
| `VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY` | deploy target |
| `GHCR_TOKEN` | push images (or `GITHUB_TOKEN` if same org) |
| `POSTGRES_PASSWORD` | db |
| `JWT_SECRET` | token signing (≥ 256-bit) |
| `MAIL_HOST/PORT/USER/PASSWORD/FROM` | password-reset email |
| `TURNSTILE_SECRET` | captcha verification |
| `CLOUDFLARE_*` | (optional) cache purge on deploy |

On the VPS: `/opt/modelmate/.env` holds the same values for `docker compose`.

## First-time VPS bootstrap  (script: `infra/bootstrap.sh`)

1. install Docker + compose plugin
2. `mkdir -p /opt/modelmate/{backups}` ; copy `docker-compose.prod.yml` + `.env`
3. hook the domain into Caddy (own Caddyfile, or add a site block to the shared one)
4. `docker compose up -d` ; verify health
5. add cron: nightly `pg_dump` + weekly prune; optional restic offsite
6. point Cloudflare DNS `A`/`AAAA` at the VPS, enable proxy

## Runbook

- **Logs:** `docker compose logs -f backend|frontend`
- **DB shell:** `docker compose exec postgres psql -U modelmate`
- **Manual migrate:** `docker compose run --rm backend flyway-migrate`
- **Restore:** `gunzip < backup.sql.gz | docker compose exec -T postgres psql -U modelmate`
- **Rollback release:** set image tags to previous SHA in `.env`, `up -d`
