#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

echo "[1/5] 启动中间件容器 (mysql/redis/rocketmq)"
docker compose up -d mysql redis rocketmq-namesrv rocketmq-broker rocketmq-dashboard >/dev/null

echo "[2/5] 等待 MySQL/Redis 健康检查"
for i in {1..40}; do
  mysql_status="$(docker inspect -f '{{.State.Health.Status}}' ptstudio-mysql 2>/dev/null || echo starting)"
  redis_status="$(docker inspect -f '{{.State.Health.Status}}' ptstudio-redis 2>/dev/null || echo starting)"
  if [[ "$mysql_status" == "healthy" && "$redis_status" == "healthy" ]]; then
    break
  fi
  sleep 2
done
if [[ "${mysql_status:-}" != "healthy" || "${redis_status:-}" != "healthy" ]]; then
  echo "MySQL/Redis 未就绪"
  exit 1
fi

echo "[3/5] 验证 Redis 连通性"
docker exec ptstudio-redis redis-cli ping | grep -q PONG

echo "[4/5] 等待 RocketMQ 端口"
for i in {1..40}; do
  if bash -c "exec 3<>/dev/tcp/127.0.0.1/9876" 2>/dev/null \
    && bash -c "exec 3<>/dev/tcp/127.0.0.1/10911" 2>/dev/null; then
    break
  fi
  sleep 2
done
if ! bash -c "exec 3<>/dev/tcp/127.0.0.1/9876" 2>/dev/null; then
  echo "RocketMQ NameServer 未就绪"
  exit 1
fi
if ! bash -c "exec 3<>/dev/tcp/127.0.0.1/10911" 2>/dev/null; then
  echo "RocketMQ Broker 未就绪"
  exit 1
fi

echo "[5/5] 输出服务地址"
echo "MySQL:   127.0.0.1:3306"
echo "Redis:   127.0.0.1:6379"
echo "RocketMQ NameServer: 127.0.0.1:9876"
echo "RocketMQ Broker:     127.0.0.1:10911"
echo "RocketMQ Dashboard:  http://127.0.0.1:8088"
echo "middleware smoke passed"
