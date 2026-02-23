#!/usr/bin/env bash
set -euo pipefail

MASTER_CONTAINER="${MASTER_CONTAINER:-ptstudio-redis-master}"
SENTINEL_PORT="${SENTINEL_PORT:-26379}"
SENTINEL_NAME="${SENTINEL_NAME:-mymaster}"

master_addr() {
  redis-cli -p "$SENTINEL_PORT" SENTINEL get-master-addr-by-name "$SENTINEL_NAME" | tr '\n' ' ' | sed 's/ $//'
}

echo "[1/6] 检查 Sentinel 当前 master"
BEFORE="$(master_addr)"
echo "  before master: $BEFORE"

echo "[2/6] 停止当前 master 容器触发故障"
docker stop "$MASTER_CONTAINER" >/dev/null

echo "[3/6] 等待 Sentinel 选主"
AFTER=""
for i in {1..60}; do
  AFTER="$(master_addr || true)"
  if [[ -n "$AFTER" && "$AFTER" != "$BEFORE" ]]; then
    break
  fi
  sleep 2
done

if [[ -z "$AFTER" || "$AFTER" == "$BEFORE" ]]; then
  echo "Sentinel 未完成主从切换"
  docker start "$MASTER_CONTAINER" >/dev/null || true
  exit 1
fi

echo "  after master: $AFTER"

echo "[4/6] 启动原 master 容器"
docker start "$MASTER_CONTAINER" >/dev/null

echo "[5/6] 验证 Sentinel 仍可查询"
FINAL="$(master_addr)"
echo "  final master: $FINAL"

echo "[6/6] 演练完成"
echo "redis sentinel failover drill passed"
