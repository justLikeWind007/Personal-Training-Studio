#!/usr/bin/env bash
set -euo pipefail

NACOS_NODE1_URL="${NACOS_NODE1_URL:-http://127.0.0.1:8848}"
NACOS_NODE2_URL="${NACOS_NODE2_URL:-http://127.0.0.1:8849}"
NACOS_NODE3_URL="${NACOS_NODE3_URL:-http://127.0.0.1:8850}"
FAIL_NODE_CONTAINER="${FAIL_NODE_CONTAINER:-ptstudio-nacos-2}"

check_ready() {
  local url="$1"
  curl -fsS "${url}/nacos/v1/console/health/readiness" >/dev/null
}

wait_ready() {
  local url="$1"
  for _ in {1..90}; do
    if check_ready "$url"; then
      return 0
    fi
    sleep 2
  done
  return 1
}

echo "[1/6] 检查演练前 Nacos 三节点健康"
check_ready "$NACOS_NODE1_URL"
check_ready "$NACOS_NODE2_URL"
check_ready "$NACOS_NODE3_URL"
echo "  - nacos-1/nacos-2/nacos-3 ready"

echo "[2/6] 停止节点容器: ${FAIL_NODE_CONTAINER}"
docker stop "${FAIL_NODE_CONTAINER}" >/dev/null

echo "[3/6] 校验其余节点可用"
for _ in {1..60}; do
  if check_ready "$NACOS_NODE1_URL" && check_ready "$NACOS_NODE3_URL"; then
    echo "  - 其余节点仍可提供服务"
    break
  fi
  sleep 2
done
check_ready "$NACOS_NODE1_URL"
check_ready "$NACOS_NODE3_URL"

echo "[4/6] 校验集群入口仍可读"
curl -fsS "${NACOS_NODE1_URL}/nacos/v1/ns/operator/servers" >/tmp/ptstudio_nacos_servers.json
grep -q '"servers"' /tmp/ptstudio_nacos_servers.json
echo "  - 集群接口查询正常"

echo "[5/6] 恢复节点容器: ${FAIL_NODE_CONTAINER}"
docker start "${FAIL_NODE_CONTAINER}" >/dev/null
if ! wait_ready "$NACOS_NODE2_URL"; then
  echo "  - 失败: 恢复后 nacos-2 未就绪"
  exit 1
fi
echo "  - nacos-2 已恢复"

echo "[6/6] 演练完成"
echo "nacos cluster failover drill passed"
