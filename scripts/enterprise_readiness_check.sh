#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

RUN_RELEASE="${RUN_RELEASE:-true}"
RUN_SECURITY="${RUN_SECURITY:-true}"
RUN_OBSERVABILITY="${RUN_OBSERVABILITY:-true}"
RUN_MIDDLEWARE="${RUN_MIDDLEWARE:-false}"

SUMMARY_FILE="${SUMMARY_FILE:-/tmp/ptstudio_enterprise_readiness_summary.txt}"
mkdir -p "$(dirname "$SUMMARY_FILE")"
: >"$SUMMARY_FILE"
FAILED=0

run_step() {
  local name="$1"
  local cmd="$2"
  local start_ts
  start_ts="$(date '+%Y-%m-%d %H:%M:%S')"
  echo "[START] ${name} @ ${start_ts}"
  if bash -lc "$cmd"; then
    echo "[PASS ] ${name}" | tee -a "$SUMMARY_FILE"
    return 0
  fi
  echo "[FAIL ] ${name}" | tee -a "$SUMMARY_FILE"
  return 1
}

echo "== 企业级发布就绪巡检 =="
echo "RUN_RELEASE=${RUN_RELEASE}"
echo "RUN_SECURITY=${RUN_SECURITY}"
echo "RUN_OBSERVABILITY=${RUN_OBSERVABILITY}"
echo "RUN_MIDDLEWARE=${RUN_MIDDLEWARE}"

if [[ "${RUN_RELEASE}" == "true" ]]; then
  if ! run_step "发布前检查" "./scripts/release_precheck.sh"; then
    FAILED=$((FAILED + 1))
  fi
fi

if [[ "${RUN_SECURITY}" == "true" ]]; then
  if ! run_step "安全基线巡检" "./scripts/security_baseline_check.sh"; then
    FAILED=$((FAILED + 1))
  fi
fi

if [[ "${RUN_OBSERVABILITY}" == "true" ]]; then
  if ! run_step "可观测性巡检" "./scripts/observability_smoke.sh"; then
    FAILED=$((FAILED + 1))
  fi
fi

if [[ "${RUN_MIDDLEWARE}" == "true" ]]; then
  if ! run_step "中间件集群巡检" "./scripts/middleware_cluster_smoke.sh"; then
    FAILED=$((FAILED + 1))
  fi
fi

echo "== 巡检完成 =="
cat "$SUMMARY_FILE"
echo "summary: $SUMMARY_FILE"
if [[ "$FAILED" -gt 0 ]]; then
  echo "enterprise readiness check failed, failedSteps=${FAILED}"
  exit 1
fi
echo "enterprise readiness check passed"
