#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
TENANT_ID="${TENANT_ID:-tenant-demo}"
STORE_ID="${STORE_ID:-store-001}"

json_request() {
  local method="$1"
  local path="$2"
  local data="${3:-}"
  if [[ -n "$data" ]]; then
    curl -sS -X "$method" "$BASE_URL$path" \
      -H "Content-Type: application/json" \
      -H "X-Tenant-Id: $TENANT_ID" \
      -H "X-Store-Id: $STORE_ID" \
      -d "$data"
  else
    curl -sS -X "$method" "$BASE_URL$path" \
      -H "X-Tenant-Id: $TENANT_ID" \
      -H "X-Store-Id: $STORE_ID"
  fi
}

echo "[1/5] health"
curl -sS "$BASE_URL/actuator/health" | grep -q '"status":"UP"'

echo "[2/5] login"
LOGIN_RESP=$(json_request POST "/api/auth/login" '{"mobile":"13800000001","password":"123456"}')
TOKEN=$(printf "%s" "$LOGIN_RESP" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
if [[ -z "$TOKEN" ]]; then
  echo "login failed: token missing"
  exit 1
fi

echo "[3/5] openapi"
curl -sS "$BASE_URL/v3/api-docs" | grep -q '"openapi"'

echo "[4/5] create lead"
LEAD_RESP=$(json_request POST "/api/leads" '{"source":"SMOKE","name":"Smoke User","mobile":"13600000999","ownerUserId":1001}')
LEAD_ID=$(printf "%s" "$LEAD_RESP" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
if [[ -z "$LEAD_ID" ]]; then
  echo "create lead failed"
  exit 1
fi

echo "[5/5] convert member"
json_request POST "/api/leads/${LEAD_ID}/convert-member" > /dev/null

echo "smoke passed"
