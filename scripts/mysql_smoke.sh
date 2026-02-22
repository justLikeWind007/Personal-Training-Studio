#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

echo "[1/6] 启动MySQL容器"
docker compose up -d mysql >/dev/null

echo "[2/6] 等待MySQL健康检查"
for i in {1..30}; do
  status=$(docker inspect -f '{{.State.Health.Status}}' ptstudio-mysql 2>/dev/null || echo "starting")
  if [[ "$status" == "healthy" ]]; then
    break
  fi
  sleep 2
done

if [[ "${status:-}" != "healthy" ]]; then
  echo "MySQL 未就绪"
  exit 1
fi

echo "[3/6] 启动服务(mysql profile)"
SPRING_PROFILES_ACTIVE=mysql mvn -q -pl ptstudio-start spring-boot:run >/tmp/ptstudio_mysql_smoke.log 2>&1 &
APP_PID=$!
trap 'kill $APP_PID >/dev/null 2>&1 || true' EXIT

for i in {1..40}; do
  if curl -sS --max-time 2 http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"'; then
    break
  fi
  sleep 2
done

if ! curl -sS --max-time 2 http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"'; then
  echo "服务未就绪，日志: /tmp/ptstudio_mysql_smoke.log"
  exit 1
fi

echo "[4/6] 检查 OpenAPI"
curl -sS --max-time 5 http://127.0.0.1:8080/v3/api-docs | grep -q '"openapi"'

echo "[5/6] 检查门店设置接口"
curl -sS --max-time 5 -H 'X-Tenant-Id: tenant-demo' -H 'X-Store-Id: store-001' \
  http://127.0.0.1:8080/api/settings/store | grep -q 'storeName'

echo "[6/6] 执行通用冒烟脚本"
BASE_URL=http://127.0.0.1:8080 TENANT_ID=tenant-demo STORE_ID=store-001 ./scripts/smoke_api.sh

echo "mysql smoke passed"
