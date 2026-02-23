#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

RUN_DIR="${RUN_DIR:-/tmp/ptstudio-ms}"
mkdir -p "$RUN_DIR"
NACOS_SERVER_ADDR="${NACOS_SERVER_ADDR:-127.0.0.1:8848}"

start_service() {
  local name="$1"
  local module="$2"
  local port="$3"
  local profiles="${4:-test}"
  local log_file="$RUN_DIR/${name}.log"
  local pid_file="$RUN_DIR/${name}.pid"

  if [[ -f "$pid_file" ]] && ps -p "$(cat "$pid_file")" >/dev/null 2>&1; then
    echo "$name already running pid=$(cat "$pid_file")"
    return
  fi

  SERVER_PORT="$port" \
  SPRING_PROFILES_ACTIVE="$profiles" \
  NACOS_SERVER_ADDR="$NACOS_SERVER_ADDR" \
  mvn -q -pl "$module" spring-boot:run >"$log_file" 2>&1 &
  echo $! >"$pid_file"
  echo "started $name pid=$(cat "$pid_file") port=$port"
}

wait_health() {
  local name="$1"
  local url="$2"
  for i in {1..80}; do
    if curl -fsS "$url" | grep -q '"status":"UP"'; then
      echo "$name healthy"
      return
    fi
    sleep 2
  done
  echo "$name health check failed: $url"
  return 1
}

echo "[1/4] start nacos container"
docker compose up -d nacos >/dev/null
for i in {1..80}; do
  if curl -fsS "http://$NACOS_SERVER_ADDR/nacos/v1/console/health/readiness" >/dev/null 2>&1; then
    echo "nacos healthy"
    break
  fi
  sleep 2
done
curl -fsS "http://$NACOS_SERVER_ADDR/nacos/v1/console/health/readiness" >/dev/null

echo "[2/4] start biz-service"
start_service "biz-service" "ptstudio-service-biz" 8081 "test"
wait_health "biz-service" "http://127.0.0.1:8081/actuator/health"

echo "[3/4] start ops-service"
start_service "ops-service" "ptstudio-service-ops" 8082 "test"
wait_health "ops-service" "http://127.0.0.1:8082/actuator/health"

echo "[4/4] start gateway"
start_service "gateway" "ptstudio-cloud-gateway" 8080 "default"
wait_health "gateway" "http://127.0.0.1:8080/actuator/health"

echo "cluster up"
echo "nacos:     http://$NACOS_SERVER_ADDR/nacos"
echo "gateway:   http://127.0.0.1:8080"
echo "biz:       http://127.0.0.1:8081"
echo "ops:       http://127.0.0.1:8082"
