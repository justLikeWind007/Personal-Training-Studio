#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

OUT_DIR="${OUT_DIR:-/tmp/ptstudio-perf-fault}"
mkdir -p "$OUT_DIR"

BASELINE_JSON="${BASELINE_JSON:-$OUT_DIR/gateway_baseline.json}"
LIMITED_JSON="${LIMITED_JSON:-$OUT_DIR/gateway_limited.json}"
FAULT_JSON="${FAULT_JSON:-$OUT_DIR/gateway_fault.json}"
REPORT_MD="${REPORT_MD:-docs/故障注入压测报告_v1.md}"

FAULT_NAME="${FAULT_NAME:-custom-fault}"
INJECT_CMD="${INJECT_CMD:-}"
RECOVER_CMD="${RECOVER_CMD:-}"

TOTAL="${TOTAL:-1200}"
CONCURRENCY="${CONCURRENCY:-80}"
TIMEOUT="${TIMEOUT:-8}"

echo "[1/6] baseline test"
python3 scripts/perf_gateway_concurrency.py \
  --total "$TOTAL" \
  --concurrency "$CONCURRENCY" \
  --timeout "$TIMEOUT" \
  --output "$BASELINE_JSON"

echo "[2/6] publish strict sentinel rules"
OPS_QPS="${OPS_QPS:-20}" GW_QPS="${GW_QPS:-30}" ./scripts/nacos_sentinel_flow_rule.sh

echo "[3/6] limited test"
python3 scripts/perf_gateway_concurrency.py \
  --total "$TOTAL" \
  --concurrency "$CONCURRENCY" \
  --timeout "$TIMEOUT" \
  --expect-limited-min 1 \
  --output "$LIMITED_JSON"

echo "[4/6] inject fault"
if [[ -n "$INJECT_CMD" ]]; then
  bash -lc "$INJECT_CMD"
else
  echo "  - no INJECT_CMD provided, skip actual fault injection"
fi

echo "[5/6] fault scenario test"
python3 scripts/perf_gateway_concurrency.py \
  --total "$TOTAL" \
  --concurrency "$CONCURRENCY" \
  --timeout "$TIMEOUT" \
  --output "$FAULT_JSON"

if [[ -n "$RECOVER_CMD" ]]; then
  echo "[recover] execute recover command"
  bash -lc "$RECOVER_CMD"
fi

echo "[6/6] generate fault injection report"
python3 scripts/perf_fault_injection_report.py \
  --baseline "$BASELINE_JSON" \
  --limited "$LIMITED_JSON" \
  --fault "$FAULT_JSON" \
  --fault-name "$FAULT_NAME" \
  --output "$REPORT_MD"

echo "baseline: $BASELINE_JSON"
echo "limited:  $LIMITED_JSON"
echo "fault:    $FAULT_JSON"
echo "report:   $REPORT_MD"
