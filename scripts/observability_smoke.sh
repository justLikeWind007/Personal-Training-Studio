#!/usr/bin/env bash
set -euo pipefail

SERVICES=(
  "gateway|http://127.0.0.1:8080"
  "biz|http://127.0.0.1:8081"
  "ops|http://127.0.0.1:8082"
)

echo "[1/3] health checks"
for item in "${SERVICES[@]}"; do
  name="${item%%|*}"
  url="${item#*|}"
  curl -fsS "${url}/actuator/health" | grep -q '"status":"UP"'
  echo "  - ${name} health ok"
done

echo "[2/3] metrics endpoint checks"
for item in "${SERVICES[@]}"; do
  name="${item%%|*}"
  url="${item#*|}"
  curl -fsS "${url}/actuator/metrics/http.server.requests" >/dev/null
  echo "  - ${name} metrics ok"
done

echo "[3/3] prometheus endpoint checks"
for item in "${SERVICES[@]}"; do
  name="${item%%|*}"
  url="${item#*|}"
  curl -fsS "${url}/actuator/prometheus" | grep -q "jvm_threads_live_threads"
  echo "  - ${name} prometheus ok"
done

echo "observability smoke passed"
