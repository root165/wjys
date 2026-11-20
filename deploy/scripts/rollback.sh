#!/usr/bin/env bash
set -Eeuo pipefail

PREVIOUS_TAG="${1:-}"
test -n "$PREVIOUS_TAG" || { echo "用法: $0 <旧镜像标签>" >&2; exit 2; }

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd -- "$SCRIPT_DIR/../.." && pwd)"
cd "$PROJECT_DIR"

ENV_FILE="${ENV_FILE:-.env}"
PROJECT_NAME="${COMPOSE_PROJECT_NAME:-wjys}"
test -f "$ENV_FILE" || { echo "找不到环境文件: $ENV_FILE" >&2; exit 1; }
IMAGE_TAG="$PREVIOUS_TAG" docker compose --env-file "$ENV_FILE" -p "$PROJECT_NAME" up -d --no-build backend frontend
"$SCRIPT_DIR/health-check.sh"
echo "已切换到镜像标签: $PREVIOUS_TAG"
