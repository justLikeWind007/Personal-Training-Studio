#!/usr/bin/env bash
set -euo pipefail

RUN_DIR="${RUN_DIR:-/tmp/ptstudio-ms}"

stop_one() {
  local name="$1"
  local pid_file="$RUN_DIR/${name}.pid"
  if [[ ! -f "$pid_file" ]]; then
    echo "$name pid file missing"
    return
  fi
  local pid
  pid="$(cat "$pid_file")"
  if ps -p "$pid" >/dev/null 2>&1; then
    kill "$pid" >/dev/null 2>&1 || true
    sleep 1
    if ps -p "$pid" >/dev/null 2>&1; then
      kill -9 "$pid" >/dev/null 2>&1 || true
    fi
    echo "stopped $name pid=$pid"
  else
    echo "$name already stopped"
  fi
  rm -f "$pid_file"
}

stop_one gateway
stop_one ops-service
stop_one biz-service
docker compose stop nacos >/dev/null 2>&1 || true

echo "cluster down"
