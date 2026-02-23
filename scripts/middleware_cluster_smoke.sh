#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

echo "[1/5] 启动集群中间件"
docker compose -f docker-compose.cluster.yml up -d mysql nacos-1 nacos-2 nacos-3 redis-master redis-replica-1 redis-replica-2 redis-sentinel-1 redis-sentinel-2 redis-sentinel-3 rocketmq-namesrv-1 rocketmq-namesrv-2 rocketmq-broker-master es01 es02 es03 kibana >/dev/null

echo "[2/5] 检查 Nacos 集群入口"
for i in {1..80}; do
  if curl -fsS http://127.0.0.1:8848/nacos/v1/console/health/readiness >/dev/null 2>&1; then
    break
  fi
  sleep 2
done
curl -fsS http://127.0.0.1:8848/nacos/v1/console/health/readiness >/dev/null

echo "[3/5] 检查 Redis Sentinel 端口"
for p in 26379 26380 26381; do
  bash -c "exec 3<>/dev/tcp/127.0.0.1/${p}" 2>/dev/null
 done

echo "[4/5] 检查 RocketMQ NameServer 端口"
for p in 19876 29876; do
  bash -c "exec 3<>/dev/tcp/127.0.0.1/${p}" 2>/dev/null
 done

echo "[5/5] 检查 ES 集群健康"
for i in {1..80}; do
  if curl -fsS http://127.0.0.1:19200/_cluster/health >/dev/null 2>&1; then
    break
  fi
  sleep 2
done
curl -fsS http://127.0.0.1:19200/_cluster/health >/dev/null

echo "cluster middleware smoke passed"
