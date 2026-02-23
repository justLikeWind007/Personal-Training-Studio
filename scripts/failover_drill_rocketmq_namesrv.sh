#!/usr/bin/env bash
set -euo pipefail

NAMESRV1_PORT="${NAMESRV1_PORT:-19876}"
NAMESRV2_PORT="${NAMESRV2_PORT:-29876}"
BROKER_PORT="${BROKER_PORT:-20911}"
FAIL_NODE_CONTAINER="${FAIL_NODE_CONTAINER:-ptstudio-rocketmq-namesrv-2}"

check_tcp_port() {
  local port="$1"
  timeout 2 bash -c "exec 3<>/dev/tcp/127.0.0.1/${port}" >/dev/null 2>&1
}

wait_port_up() {
  local port="$1"
  for _ in {1..90}; do
    if check_tcp_port "$port"; then
      return 0
    fi
    sleep 2
  done
  return 1
}

echo "[1/6] 检查演练前 NameServer 与 Broker 端口"
check_tcp_port "$NAMESRV1_PORT"
check_tcp_port "$NAMESRV2_PORT"
check_tcp_port "$BROKER_PORT"
echo "  - namesrv-1/namesrv-2/broker 端口可达"

echo "[2/6] 停止 NameServer 节点: ${FAIL_NODE_CONTAINER}"
docker stop "${FAIL_NODE_CONTAINER}" >/dev/null

echo "[3/6] 校验剩余 NameServer 与 Broker 仍可达"
for _ in {1..60}; do
  if check_tcp_port "$NAMESRV1_PORT" && check_tcp_port "$BROKER_PORT"; then
    echo "  - namesrv-1 与 broker 持续可达"
    break
  fi
  sleep 2
done
check_tcp_port "$NAMESRV1_PORT"
check_tcp_port "$BROKER_PORT"

echo "[4/6] 校验故障节点端口已不可达"
if check_tcp_port "$NAMESRV2_PORT"; then
  echo "  - 失败: namesrv-2 停止后端口仍可达"
  docker start "${FAIL_NODE_CONTAINER}" >/dev/null || true
  exit 1
fi
echo "  - namesrv-2 已下线"

echo "[5/6] 恢复 NameServer 节点: ${FAIL_NODE_CONTAINER}"
docker start "${FAIL_NODE_CONTAINER}" >/dev/null
if ! wait_port_up "$NAMESRV2_PORT"; then
  echo "  - 失败: 恢复后 namesrv-2 端口未就绪"
  exit 1
fi
echo "  - namesrv-2 已恢复"

echo "[6/6] 演练完成"
echo "rocketmq namesrv failover drill passed"
