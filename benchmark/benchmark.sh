#!/usr/bin/env bash

set -euo pipefail

cd "$(dirname "$0")"

if [[ $# -lt 2 ]]; then
    echo "Usage: ./benchmark.sh SCALE RPS"
    echo "Example:"
    echo "./benchmark.sh 5 500"
    echo "./benchmark.sh 10 1000"
    exit 1
fi

SCALE="$1"
RATE="$2"

if [[ ! -f benchmark-results.csv ]]; then
    echo "Scale,RPS,P95ms,P99ms" > benchmark-results.csv
fi

echo
echo "===================================================="
echo "Distributed Rate Limiter Benchmark"
echo "===================================================="
echo "Scale      : $SCALE"
echo "Target RPS : $RATE"
echo "===================================================="
echo

rm -f result.json

echo
echo "Running k6 benchmark..."
echo

k6 run -e RATE="$RATE" load-test.js --summary-export=result.json

if [[ ! -f result.json ]]; then
    echo "result.json was not generated."
    exit 1
fi

RPS=$(jq -r '.metrics.http_reqs.rate' result.json)
P95=$(jq -r '.metrics.http_req_duration.values["p(95)"]' result.json)
P99=$(jq -r '.metrics.http_req_duration.values["p(99)"]' result.json)

printf "%s,%.2f,%.2f,%.2f\n" \
    "$SCALE" \
    "$RPS" \
    "$P95" \
    "$P99" >> benchmark-results.csv

echo
echo "===================================================="
echo "Results appended successfully"
echo "===================================================="
echo

cat benchmark-results.csv

echo
echo "Done."

exit 0