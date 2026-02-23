#!/usr/bin/env bash
set -euo pipefail

ES_URL="${ES_URL:-http://127.0.0.1:19200}"
FAIL_NODE="${FAIL_NODE:-ptstudio-es02}"
DRILL_INDEX="${DRILL_INDEX:-ptstudio-drill-index}"

extract_field() {
  local json="$1"
  local field="$2"
  echo "$json" | sed -n "s/.*\"${field}\":[[:space:]]*\"\\{0,1\\}\\([^\",}]*\\)\"\\{0,1\\}.*/\\1/p"
}

cluster_health() {
  curl -fsS "${ES_URL}/_cluster/health"
}

wait_nodes() {
  local expect="$1"
  for _ in {1..90}; do
    local h
    h="$(cluster_health || true)"
    local n
    n="$(extract_field "$h" "number_of_nodes")"
    if [[ "$n" == "$expect" ]]; then
      echo "$h"
      return 0
    fi
    sleep 2
  done
  return 1
}

echo "[1/7] 检查演练前集群健康"
before="$(cluster_health)"
before_nodes="$(extract_field "$before" "number_of_nodes")"
before_status="$(extract_field "$before" "status")"
echo "  before: status=${before_status}, nodes=${before_nodes}"
if [[ -z "${before_nodes}" || "${before_nodes}" -lt 3 ]]; then
  echo "  - 失败: 演练前节点数不足 3"
  exit 1
fi

echo "[2/7] 写入演练数据"
curl -fsS -X PUT "${ES_URL}/${DRILL_INDEX}" \
  -H "Content-Type: application/json" \
  -d '{"settings":{"number_of_shards":1,"number_of_replicas":1}}' >/dev/null || true
curl -fsS -X POST "${ES_URL}/${DRILL_INDEX}/_doc/1?refresh=true" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"es-failover-drill\",\"ts\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"}" >/dev/null
echo "  - 演练数据写入成功"

echo "[3/7] 停止单节点容器: ${FAIL_NODE}"
docker stop "${FAIL_NODE}" >/dev/null

echo "[4/7] 等待集群降为 2 节点"
after_stop="$(wait_nodes 2 || true)"
if [[ -z "${after_stop}" ]]; then
  echo "  - 失败: 停止节点后未检测到 2 节点状态"
  docker start "${FAIL_NODE}" >/dev/null || true
  exit 1
fi
after_stop_status="$(extract_field "$after_stop" "status")"
echo "  after-stop: status=${after_stop_status}, nodes=2"
if [[ "${after_stop_status}" == "red" ]]; then
  echo "  - 失败: 集群进入 red"
  docker start "${FAIL_NODE}" >/dev/null || true
  exit 1
fi

echo "[5/7] 校验故障期间读写可用"
curl -fsS -X POST "${ES_URL}/${DRILL_INDEX}/_doc/2?refresh=true" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"es-failover-drill-2\",\"ts\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"}" >/dev/null
curl -fsS "${ES_URL}/${DRILL_INDEX}/_doc/1" | grep -q '"found":true'
echo "  - 读写校验通过"

echo "[6/7] 恢复节点: ${FAIL_NODE}"
docker start "${FAIL_NODE}" >/dev/null
after_recover="$(wait_nodes 3 || true)"
if [[ -z "${after_recover}" ]]; then
  echo "  - 失败: 恢复后未回到 3 节点"
  exit 1
fi
after_recover_status="$(extract_field "$after_recover" "status")"
echo "  after-recover: status=${after_recover_status}, nodes=3"

echo "[7/7] 演练完成"
echo "es cluster failover drill passed"
