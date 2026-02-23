#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

OUT_DIR="${OUT_DIR:-/tmp/ptstudio-perf}"
mkdir -p "$OUT_DIR"

BASE_TOTAL="${BASE_TOTAL:-1200}"
BASE_CONCURRENCY="${BASE_CONCURRENCY:-80}"
LIMITED_TOTAL="${LIMITED_TOTAL:-1200}"
LIMITED_CONCURRENCY="${LIMITED_CONCURRENCY:-80}"

BASELINE_JSON="$OUT_DIR/gateway_baseline.json"
LIMITED_JSON="$OUT_DIR/gateway_limited.json"
REPORT_MD="$OUT_DIR/gateway_compare_report.md"

echo "[1/4] baseline test (no explicit sentinel rule)"
python3 scripts/perf_gateway_concurrency.py \
  --total "$BASE_TOTAL" \
  --concurrency "$BASE_CONCURRENCY" \
  --output "$BASELINE_JSON"

echo "[2/4] publish strict sentinel rules"
OPS_QPS="${OPS_QPS:-20}" GW_QPS="${GW_QPS:-30}" ./scripts/nacos_sentinel_flow_rule.sh

echo "[3/4] limited test"
python3 scripts/perf_gateway_concurrency.py \
  --total "$LIMITED_TOTAL" \
  --concurrency "$LIMITED_CONCURRENCY" \
  --expect-limited-min 1 \
  --output "$LIMITED_JSON"

echo "[4/4] generate compare report"
python3 scripts/perf_gateway_compare_report.py \
  --baseline "$BASELINE_JSON" \
  --limited "$LIMITED_JSON" \
  --output "$REPORT_MD"

echo "baseline: $BASELINE_JSON"
echo "limited:  $LIMITED_JSON"
echo "report:   $REPORT_MD"
