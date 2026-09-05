# Deployment & CI/CD

Artifacts:

| file | purpose |
|---|---|
| `backend/Dockerfile` | multi-stage JDK build → JRE 21 runtime, non-root, actuator healthcheck |
| `frontend/Dockerfile` | multi-stage pnpm build → Next.js `standalone` runtime, non-root |
| `docker-compose.prod.yml` | postgres + backend + frontend (+ `caddy` under `--profile edge`) |
| `infra/Caddyfile` | dedicated edge proxy (only with `--profile edge`) |
| `infra/Caddyfile.shared-snippet` | site block to paste into the VPS's shared Caddy |
| `infra/bootstrap.sh` | idempotent first-time VPS setup + backup cron |
| `.env.prod.example` | template for `/opt/modelmate/.env` |
| `.github/workflows/ci.yml` | lint / typecheck / test / build, path-filtered |
| `.github/workflows/deploy.yml` | build+push GHCR images, SSH deploy, health gate |

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

## First-time VPS bootstrap

```bash
# from a clone of this repo, on your machine:
scp -r docker-compose.prod.yml infra .env.prod.example <vps>:/tmp/mm/
ssh <vps> 'sudo bash /tmp/mm/infra/bootstrap.sh'      # 1st run: creates /opt/modelmate/.env, stops
ssh <vps> 'sudo nano /opt/modelmate/.env'             # set POSTGRES_PASSWORD, JWT_SECRET, SITE_URL
ssh <vps> 'sudo bash /tmp/mm/infra/bootstrap.sh'      # 2nd run: Caddy site block, compose up, backup cron
```

`bootstrap.sh` installs Docker if missing, appends `infra/Caddyfile.shared-snippet`
to `/etc/caddy/Caddyfile` (reloading Caddy), starts the stack, and installs the
nightly `pg_dump` cron (14-day retention).

Then in Cloudflare: point `modelmate.mmi404.com` `A`/`AAAA` at the VPS, proxied (orange cloud).

## GitHub configuration

Secrets (repo → Settings → Secrets and variables → Actions):
`VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY`, `GHCR_TOKEN` (a PAT with `write:packages`).
Variable: `SITE_URL` (defaults to `https://modelmate.mmi404.com`).
The `deploy` job uses a `production` environment — add required reviewers there if you want a manual gate.

## Runbook

- **Logs:** `docker compose logs -f backend|frontend`
- **DB shell:** `docker compose exec postgres psql -U modelmate`
- **Manual migrate:** `docker compose run --rm backend flyway-migrate`
- **Restore:** `gunzip < backup.sql.gz | docker compose exec -T postgres psql -U modelmate`
- **Rollback release:** set image tags to previous SHA in `.env`, `up -d`
