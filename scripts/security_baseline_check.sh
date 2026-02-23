#!/usr/bin/env bash
set -euo pipefail

GATEWAY_BASE_URL="${GATEWAY_BASE_URL:-http://127.0.0.1:8080}"
BIZ_BASE_URL="${BIZ_BASE_URL:-http://127.0.0.1:8081}"
OPS_BASE_URL="${OPS_BASE_URL:-http://127.0.0.1:8082}"
TENANT_ID="${TENANT_ID:-tenant-demo}"
STORE_ID="${STORE_ID:-store-001}"
MOBILE="${MOBILE:-13800000001}"
PASSWORD="${PASSWORD:-123456}"
INTERNAL_TOKEN_VALUE="${INTERNAL_TOKEN_VALUE:-}"
INTERNAL_TOKEN_REQUIRED="${INTERNAL_TOKEN_REQUIRED:-false}"

tmp_headers="$(mktemp)"
trap 'rm -f "$tmp_headers"' EXIT

echo "[1/4] 网关链路登录检查"
curl -fsS -D "$tmp_headers" -o /tmp/ptstudio_security_login.json \
  -X POST "${GATEWAY_BASE_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: ${TENANT_ID}" \
  -H "X-Store-Id: ${STORE_ID}" \
  -d "{\"mobile\":\"${MOBILE}\",\"password\":\"${PASSWORD}\"}" >/dev/null
grep -q '"token"' /tmp/ptstudio_security_login.json
echo "  - 登录接口可用"

echo "[2/4] 追踪头校验"
grep -qi '^X-Request-Id:' "$tmp_headers"
grep -qi '^X-Trace-Id:' "$tmp_headers"
echo "  - 响应头包含 X-Request-Id / X-Trace-Id"

echo "[3/4] 业务服务健康检查"
curl -fsS "${BIZ_BASE_URL}/actuator/health" | grep -q '"status":"UP"'
curl -fsS "${OPS_BASE_URL}/actuator/health" | grep -q '"status":"UP"'
echo "  - biz/ops health 均为 UP"

echo "[4/4] 内部令牌强校验检查"
if [[ "${INTERNAL_TOKEN_REQUIRED}" == "true" ]]; then
  code_no_token="$(curl -s -o /tmp/ptstudio_security_direct_no_token.json -w '%{http_code}' \
    -X POST "${BIZ_BASE_URL}/api/auth/login" \
    -H "Content-Type: application/json" \
    -H "X-Tenant-Id: ${TENANT_ID}" \
    -H "X-Store-Id: ${STORE_ID}" \
    -d "{\"mobile\":\"${MOBILE}\",\"password\":\"${PASSWORD}\"}")"
  if [[ "${code_no_token}" == "200" ]]; then
    echo "  - 失败: 强校验开启时，直连 biz 不带令牌不应成功"
    exit 1
  fi
  if [[ -n "${INTERNAL_TOKEN_VALUE}" ]]; then
    code_with_token="$(curl -s -o /tmp/ptstudio_security_direct_with_token.json -w '%{http_code}' \
      -X POST "${BIZ_BASE_URL}/api/auth/login" \
      -H "Content-Type: application/json" \
      -H "X-Tenant-Id: ${TENANT_ID}" \
      -H "X-Store-Id: ${STORE_ID}" \
      -H "X-Internal-Token: ${INTERNAL_TOKEN_VALUE}" \
      -d "{\"mobile\":\"${MOBILE}\",\"password\":\"${PASSWORD}\"}")"
    if [[ "${code_with_token}" != "200" ]]; then
      echo "  - 失败: 强校验开启后，带正确内部令牌应可访问直连链路"
      exit 1
    fi
    echo "  - 强校验模式校验通过（拒绝无令牌 + 放行正确令牌）"
  else
    echo "  - 提示: INTERNAL_TOKEN_VALUE 未设置，仅校验了无令牌拒绝行为"
  fi
else
  echo "  - INTERNAL_TOKEN_REQUIRED=false，跳过强校验断言"
fi

echo "security baseline check passed"
