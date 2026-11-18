#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd -- "$SCRIPT_DIR/../.." && pwd)"
cd "$PROJECT_DIR"

ENV_FILE="${ENV_FILE:-.env}"
PROJECT_NAME="${COMPOSE_PROJECT_NAME:-wjys}"

test -f "$ENV_FILE" || { echo "找不到环境文件: $ENV_FILE" >&2; exit 1; }
docker compose --env-file "$ENV_FILE" -p "$PROJECT_NAME" config --quiet
docker compose --env-file "$ENV_FILE" -p "$PROJECT_NAME" up -d --build
"$SCRIPT_DIR/health-check.sh"
