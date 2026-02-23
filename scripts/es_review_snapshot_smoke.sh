#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

BASE_URL="${BASE_URL:-http://127.0.0.1:18081}"
TENANT_ID="${TENANT_ID:-tenant-demo}"
STORE_ID="${STORE_ID:-store-001}"
LOG_FILE="${LOG_FILE:-/tmp/ptstudio_es_snapshot_smoke.log}"

json_request() {
  local method="$1"
  local path="$2"
  local data="${3:-}"
  if [[ -n "$data" ]]; then
    curl -fsS -X "$method" "$BASE_URL$path" \
      -H "Content-Type: application/json" \
      -H "X-Tenant-Id: $TENANT_ID" \
      -H "X-Store-Id: $STORE_ID" \
      -d "$data"
  else
    curl -fsS -X "$method" "$BASE_URL$path" \
      -H "X-Tenant-Id: $TENANT_ID" \
      -H "X-Store-Id: $STORE_ID"
  fi
}

echo "[1/6] 准备中间件环境"
./scripts/middleware_smoke.sh >/dev/null

echo "[2/6] 启动应用 (mysql,redis,mq,es)"
SERVER_PORT=18081 SPRING_PROFILES_ACTIVE=mysql,redis,mq,es mvn -q -pl ptstudio-start spring-boot:run >"$LOG_FILE" 2>&1 &
APP_PID=$!
cleanup() {
  if ps -p "$APP_PID" >/dev/null 2>&1; then
    kill "$APP_PID" >/dev/null 2>&1 || true
    wait "$APP_PID" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

echo "[3/6] 等待应用健康检查"
for i in {1..80}; do
  if curl -fsS "$BASE_URL/actuator/health" | grep -q '"status":"UP"'; then
    break
  fi
  sleep 2
done
curl -fsS "$BASE_URL/actuator/health" | grep -q '"status":"UP"'

echo "[4/6] 触发复盘快照归档"
SNAPSHOT_RESP="$(json_request GET "/api/ops/review-dashboard")"
printf "%s" "$SNAPSHOT_RESP" | grep -q '"storeId":"'
printf "%s" "$SNAPSHOT_RESP" | grep -q '"completionRate"'

echo "[5/6] 校验 latest 接口"
LATEST_RESP="$(json_request GET "/api/ops/review-dashboard/latest")"
printf "%s" "$LATEST_RESP" | grep -q '"tenantId":"'$TENANT_ID'"'
printf "%s" "$LATEST_RESP" | grep -q '"storeId":"'$STORE_ID'"'
printf "%s" "$LATEST_RESP" | grep -q '"generatedAt"'

echo "[6/6] 校验 ES 索引文档"
DOC_ID="${TENANT_ID}_${STORE_ID}"
ES_DOC="$(curl -fsS "http://127.0.0.1:9200/ptstudio_ops_review_snapshot/_doc/${DOC_ID}")"
printf "%s" "$ES_DOC" | grep -q '"found":true'
printf "%s" "$ES_DOC" | grep -q '"tenantId":"'$TENANT_ID'"'
printf "%s" "$ES_DOC" | grep -q '"storeId":"'$STORE_ID'"'

echo "es review snapshot smoke passed"
