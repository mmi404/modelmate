#!/usr/bin/env bash
# First-time setup for ModelMate on the VPS. Idempotent — safe to re-run.
#
#   scp -r docker-compose.prod.yml infra .env.prod.example <vps>:/tmp/modelmate-setup/
#   ssh <vps> 'sudo bash /tmp/modelmate-setup/infra/bootstrap.sh'
#
# Assumes the VPS already runs a shared Caddy on the host (see O-2 / ADR-009).
set -euo pipefail

APP_DIR=/opt/modelmate
SETUP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "==> Docker"
if ! command -v docker >/dev/null 2>&1; then
  curl -fsSL https://get.docker.com | sh
  systemctl enable --now docker
fi
docker compose version >/dev/null

echo "==> Directories"
mkdir -p "$APP_DIR/backups"
cp "$SETUP_DIR/docker-compose.prod.yml" "$APP_DIR/"
mkdir -p "$APP_DIR/infra"
cp "$SETUP_DIR/infra/Caddyfile" "$APP_DIR/infra/" 2>/dev/null || true

if [ ! -f "$APP_DIR/.env" ]; then
  cp "$SETUP_DIR/.env.prod.example" "$APP_DIR/.env"
  echo "!! Edit $APP_DIR/.env — set POSTGRES_PASSWORD, JWT_SECRET, SITE_URL, then re-run."
  exit 0
fi

echo "==> Shared Caddy site block"
CADDYFILE=/etc/caddy/Caddyfile
if [ -f "$CADDYFILE" ] && ! grep -q "modelmate.mmi404.com" "$CADDYFILE"; then
  {
    echo ""
    echo "# --- ModelMate (added by infra/bootstrap.sh) ---"
    cat "$SETUP_DIR/infra/Caddyfile.shared-snippet"
  } >> "$CADDYFILE"
  caddy validate --config "$CADDYFILE" && systemctl reload caddy
  echo "   site block appended and Caddy reloaded"
else
  echo "   already present (or no shared Caddy) — skipping"
fi

echo "==> Start the stack"
cd "$APP_DIR"
docker compose -f docker-compose.prod.yml pull || true
docker compose -f docker-compose.prod.yml up -d

echo "==> Nightly pg_dump cron"
CRON_LINE="15 3 * * * cd $APP_DIR && docker compose -f docker-compose.prod.yml exec -T postgres pg_dump -U \${POSTGRES_USER:-modelmate} \${POSTGRES_DB:-modelmate} | gzip > $APP_DIR/backups/modelmate-\$(date +\\%F).sql.gz && find $APP_DIR/backups -name '*.sql.gz' -mtime +14 -delete"
( crontab -l 2>/dev/null | grep -v 'modelmate/backups' ; echo "$CRON_LINE" ) | crontab -
echo "   installed"

echo "==> Done. Check: docker compose -f $APP_DIR/docker-compose.prod.yml ps"
