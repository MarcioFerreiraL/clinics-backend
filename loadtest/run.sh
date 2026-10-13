#!/usr/bin/env bash
# Roda um cenário k6 contra o ambiente de carga e guarda o resumo em loadtest/results/.
# Uso: loadtest/run.sh <smoke|load|stress|spike|soak> [BASE_URL]
set -euo pipefail
cd "$(dirname "$0")/.."

SCENARIO="${1:?uso: run.sh smoke|load|stress|spike|soak [base-url]}"
BASE_URL="${2:-http://localhost:8091}"
mkdir -p loadtest/results

docker run --rm --user "$(id -u):$(id -g)" --network host -v "$PWD/loadtest:/work" -w /work \
  -e BASE_URL="$BASE_URL" -e DURATION="${DURATION:-10m}" \
  grafana/k6:latest run --quiet --summary-export "results/${SCENARIO}.json" "${SCENARIO}.js"
