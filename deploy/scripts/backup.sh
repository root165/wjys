#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd -- "$SCRIPT_DIR/../.." && pwd)"
cd "$PROJECT_DIR"

ENV_FILE="${ENV_FILE:-.env}"
PROJECT_NAME="${COMPOSE_PROJECT_NAME:-wjys}"
BACKUP_DIR="${BACKUP_DIR:-/data/wjys/backups}"
mkdir -p "$BACKUP_DIR"
test -f "$ENV_FILE" || { echo "找不到环境文件: $ENV_FILE" >&2; exit 1; }

STAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP_FILE="$BACKUP_DIR/wjys-$STAMP.sql.gz"
docker compose --env-file "$ENV_FILE" -p "$PROJECT_NAME" exec -T mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' | gzip > "$BACKUP_FILE"
chmod 600 "$BACKUP_FILE"
echo "MySQL 备份已生成: $BACKUP_FILE"
