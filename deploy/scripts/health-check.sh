#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd -- "$SCRIPT_DIR/../.." && pwd)"
cd "$PROJECT_DIR"

ENV_FILE="${ENV_FILE:-.env}"
PROJECT_NAME="${COMPOSE_PROJECT_NAME:-wjys}"
BASE_URL="${1:-http://127.0.0.1:18080}"

test -f "$ENV_FILE" || { echo "找不到环境文件: $ENV_FILE" >&2; exit 1; }
curl --fail --silent --show-error --max-time 10 "$BASE_URL/" >/dev/null
BACKEND_HEALTH="$(docker compose --env-file "$ENV_FILE" -p "$PROJECT_NAME" exec -T backend curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health)"
printf '%s\n' "$BACKEND_HEALTH" | grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"' || {
  echo "后端健康状态不是 UP: $BACKEND_HEALTH" >&2
  exit 1
}
echo "frontend: UP ($BASE_URL)"
echo "backend: UP"
