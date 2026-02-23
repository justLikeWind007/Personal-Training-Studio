#!/usr/bin/env bash
set -euo pipefail

GATEWAY_BASE_URL="${GATEWAY_BASE_URL:-http://127.0.0.1:8080}"
BIZ_BASE_URL="${BIZ_BASE_URL:-http://127.0.0.1:8081}"
OPS_BASE_URL="${OPS_BASE_URL:-http://127.0.0.1:8082}"
NACOS_SERVER_ADDR="${NACOS_SERVER_ADDR:-127.0.0.1:8848}"

check_json_up() {
  local name="$1"
  local url="$2"
  curl -fsS "$url" | grep -q '"status":"UP"'
  echo "  - ${name} UP"
}

echo "[1/6] 检查 Nacos"
curl -fsS "http://${NACOS_SERVER_ADDR}/nacos/v1/console/health/readiness" >/dev/null
echo "  - nacos ready"

echo "[2/6] 检查服务健康"
check_json_up "gateway" "${GATEWAY_BASE_URL}/actuator/health"
check_json_up "biz" "${BIZ_BASE_URL}/actuator/health"
check_json_up "ops" "${OPS_BASE_URL}/actuator/health"

echo "[3/6] 检查关键指标端点"
for u in "$GATEWAY_BASE_URL" "$BIZ_BASE_URL" "$OPS_BASE_URL"; do
  curl -fsS "$u/actuator/prometheus" >/dev/null
done
echo "  - prometheus endpoints ok"

echo "[4/6] 检查网关关键链路"
TENANT_ID="${TENANT_ID:-tenant-demo}"
STORE_ID="${STORE_ID:-store-001}"
curl -fsS -X POST "${GATEWAY_BASE_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: ${TENANT_ID}" \
  -H "X-Store-Id: ${STORE_ID}" \
  -d '{"mobile":"13800000001","password":"123456"}' | grep -q '"token"'
echo "  - gateway login route ok"

echo "[5/6] 检查工作区状态"
if [[ -n "$(git status --short 2>/dev/null || true)" ]]; then
  echo "  - 警告: 当前工作区存在未提交改动（建议发布前清理）"
else
  echo "  - git worktree clean"
fi

echo "[6/6] 输出发布建议"
echo "precheck passed"
echo "建议后续执行:"
echo "  1) 小流量灰度（5%-10%）"
echo "  2) 观察 10-15 分钟核心告警"
echo "  3) 放量到 50% 后再全量"
