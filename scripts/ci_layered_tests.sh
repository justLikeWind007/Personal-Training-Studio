#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LAYER="${1:-all}"

cd "${ROOT_DIR}"

run_mvn() {
  local profile="$1"
  echo "[ci-layered-tests] run profile=${profile}"
  mvn -q -pl ptstudio-start -am -P"${profile}" test
}

case "${LAYER}" in
  smoke)
    run_mvn ci-smoke
    ;;
  security)
    run_mvn ci-security
    ;;
  regression)
    run_mvn ci-regression
    ;;
  all)
    run_mvn ci-smoke
    run_mvn ci-security
    run_mvn ci-regression
    ;;
  *)
    echo "Usage: $0 [smoke|security|regression|all]" >&2
    exit 1
    ;;
esac

echo "[ci-layered-tests] done layer=${LAYER}"
