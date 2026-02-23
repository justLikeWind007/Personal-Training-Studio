#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
TENANT_ID="${TENANT_ID:-tenant-demo}"
STORE_ID="${STORE_ID:-store-001}"
NACOS_SERVER_ADDR="${NACOS_SERVER_ADDR:-127.0.0.1:8848}"

echo "[1/4] nacos health"
curl -fsS "http://${NACOS_SERVER_ADDR}/nacos/v1/console/health/readiness" >/dev/null

echo "[2/4] gateway health"
curl -fsS "$BASE_URL/actuator/health" | grep -q '"status":"UP"'

echo "[3/4] biz route check"
LOGIN_RESP=$(curl -fsS -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Store-Id: $STORE_ID" \
  -d '{"mobile":"13800000001","password":"123456"}')
printf "%s" "$LOGIN_RESP" | grep -q '"token"'

echo "[4/4] ops route check"
HEALTH_RESP=$(curl -fsS "$BASE_URL/api/ops/async-queue/health" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Store-Id: $STORE_ID")
printf "%s" "$HEALTH_RESP" | grep -q '"status"'

echo "microservice smoke passed"
