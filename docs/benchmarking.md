# Benchmarking

## Purpose

This document explains how to prepare the environment, scale the Docker deployment, run load tests, and capture benchmark results for the rate limiter.

## What is being benchmarked

The repository already includes a k6-based benchmark targeting:

- `http://localhost:8084/api/payments/healthcheck`

The benchmark:

- sends `GET` requests through NGINX to the gateway
- uses tenant tier header `X-Tenant-Tier: FREE`
- treats both `200` and `429` as valid outcomes
- runs with a constant arrival rate for `2m`

This means the benchmark measures system behavior under controlled throttling, not only raw success throughput.

## Benchmark prerequisites

Required:

- Docker
- Docker Compose support
- k6

Recommended:

- `jq`
- `curl`

Notes:

- `benchmark/benchmark.sh` requires both `k6` and `jq`.
- `benchmark/benchmark.bat` requires `k6` on Windows and uses PowerShell to parse results.

## Files involved

- `benchmark/load-test.js`
- `benchmark/benchmark.sh`
- `benchmark/benchmark.bat`
- `docker-compose.yml`
- `monitoring/grafana/dashboards/rate-limiter-benchmark.json`

## Benchmark topology

Traffic path:

1. k6 sends requests to `localhost:8084`
2. NGINX forwards to `api-gateway`
3. `api-gateway` applies rate limiting
4. allowed requests are forwarded to `payment-service`
5. metrics are scraped by Prometheus and visualized in Grafana

## Start the environment

### Baseline single-instance stack

```bash
docker compose up --build -d
```

Verify startup:

```bash
docker compose ps
curl -i http://localhost:8084/api/payments/healthcheck
curl -s http://localhost:8085/admin/policies
```

## Scale the benchmark target

The current Compose file supports scaling these stateless services:

- `api-gateway`
- `payment-service`
- `order-service`

These are not practical scaling targets in the current file:

- `nginx`
- `configuration-server`
- `redis`
- `postgres-policy`
- `prometheus`
- `grafana`
- `jaeger`

Reasons:

- some services are singleton infrastructure by design
- some services use fixed `container_name`, which conflicts with Compose scaling

### Scale gateway only

```bash
docker compose up --build -d --scale api-gateway=3
```

### Scale gateway and downstream services

```bash
docker compose up --build -d \
  --scale api-gateway=3 \
  --scale payment-service=2 \
  --scale order-service=2
```

### Higher-scale example

```bash
docker compose up --build -d \
  --scale api-gateway=5 \
  --scale payment-service=3 \
  --scale order-service=3
```

## Why gateway scaling matters here

The benchmark exercises the distributed limiter design. Scaling `api-gateway` is the main way to test:

- how well local token buckets plus Redis leasing coordinate across instances
- how much Redis protects global fairness across multiple gateway nodes
- whether degraded mode still behaves predictably when multiple gateways are active

NGINX forwards to the `api-gateway` service name, so Docker DNS-based service discovery is what spreads traffic across the scaled gateway containers.

## Pre-run checks

Before each run:

```bash
docker compose ps
curl -i http://localhost:8084/api/payments/healthcheck
docker compose exec api-gateway curl -s http://localhost:9091/actuator/health
```

If running multiple gateway containers, inspect them:

```bash
docker compose ps api-gateway
docker compose logs --tail=100 api-gateway
```

Confirm monitoring is up:

```bash
curl -s http://localhost:9090/-/ready
curl -s http://localhost:3000/api/health
```

## Run the benchmark

## Option 1: Use the provided script

From the repository root:

```bash
cd benchmark
./benchmark.sh 3 500
```

Arguments:

- first argument: scale label to record in CSV
- second argument: target request rate per second

Example:

```bash
cd benchmark
./benchmark.sh 5 1000
```

What the script does:

- executes k6 with `RATE=<RPS>`
- exports summary output to `result.json`
- extracts actual RPS, p95 latency, and p99 latency
- appends a row to `benchmark-results.csv`

## Option 2: Run k6 directly

```bash
k6 run -e RATE=500 benchmark/load-test.js
```

Use this when you want the raw k6 output without CSV aggregation.

## Suggested benchmark progression

Run a staircase instead of jumping straight to the highest load:

1. `docker compose up --build -d --scale api-gateway=1`
2. `cd benchmark && ./benchmark.sh 1 200`
3. `cd benchmark && ./benchmark.sh 1 500`
4. `docker compose up --build -d --scale api-gateway=3 --scale payment-service=2 --scale order-service=2`
5. `cd benchmark && ./benchmark.sh 3 500`
6. `cd benchmark && ./benchmark.sh 3 1000`
7. `docker compose up --build -d --scale api-gateway=5 --scale payment-service=3 --scale order-service=3`
8. `cd benchmark && ./benchmark.sh 5 1000`
9. `cd benchmark && ./benchmark.sh 5 1500`

This gives comparable data points across scale levels.

## Result files

The benchmark folder can produce:

- `benchmark-results.csv`
- `result.json`

`benchmark-results.csv` columns:

- `Scale`
- `RPS`
- `P95ms`
- `P99ms`

## How to interpret results

### `200` and `429` both matter

This system is a rate limiter. A high number of `429` responses under pressure is not automatically a failure.

What to look for:

- stable actual RPS close to the target rate
- reasonable p95 and p99 latency
- predictable throttling behavior
- no widespread `5xx` errors

### Signals of a healthy run

- k6 threshold `http_req_failed: rate < 0.05` passes
- k6 threshold `http_req_duration: p(95) < 500` passes
- responses are mostly `200` and `429`
- gateway remains healthy
- Prometheus continues scraping targets

### Signals of trouble

- increasing `5xx` responses
- very high p95 or p99 latency
- gateway health failures
- Redis health degrading repeatedly
- large gap between target RPS and actual achieved RPS

## What to observe during the run

### Prometheus

Use:

- `http://localhost:9090`

Watch for gateway metrics such as:

- `rate_limiter.decisions`
- `rate_limiter.redis.fallbacks`
- `rate_limiter.config.sync`
- `redis.health_checks`
- `rate_limiter.local.tracked_buckets`

### Grafana

Use:

- `http://localhost:3000`

Dashboard assets in the repo include a benchmark dashboard and a rate limiter dashboard.

### Jaeger

Use:

- `http://localhost:16686`

Useful for:

- spotting downstream latency growth
- seeing whether traces remain intact under load

## Example benchmark session

### 1. Start scaled environment

```bash
docker compose up --build -d \
  --scale api-gateway=3 \
  --scale payment-service=2 \
  --scale order-service=2
```

### 2. Verify services

```bash
docker compose ps
curl -i http://localhost:8084/api/payments/healthcheck
```

### 3. Run benchmark

```bash
cd benchmark
./benchmark.sh 3 1000
```

### 4. Inspect results

```bash
cat benchmark-results.csv
```

### 5. Inspect logs if needed

```bash
docker compose logs --tail=200 api-gateway
docker compose logs --tail=200 redis
```

## Reset between runs

If you want to keep containers and only rerun k6:

```bash
rm -f benchmark/result.json
```

If you want to restart cleanly:

```bash
docker compose down
docker compose up --build -d --scale api-gateway=3 --scale payment-service=2 --scale order-service=2
```

If you want to remove persisted database state as well:

```bash
docker compose down -v
```

Use `-v` only when you intentionally want to reset Postgres data.

## Troubleshooting benchmark runs

## `k6: command not found`

Install k6 and rerun the benchmark.

## `jq: command not found`

Use direct k6 execution or install `jq` so `benchmark.sh` can extract metrics into CSV.

## Benchmark script runs but no `result.json`

Likely causes:

- k6 execution failed
- target endpoint was unreachable

Check:

```bash
curl -i http://localhost:8084/api/payments/healthcheck
docker compose ps
```

## Scaling command fails

Likely causes:

- attempting to scale a service with fixed `container_name`
- stale containers from a previous incompatible run

Use supported scaling targets only:

- `api-gateway`
- `payment-service`
- `order-service`

## High `5xx` rate

Check:

- `docker compose logs api-gateway`
- `docker compose logs payment-service`
- `docker compose logs order-service`
- `docker compose logs redis`

## Benchmark notes specific to this repository

- The provided k6 scenario currently exercises only the payment route.
- The load test uses the `FREE` tier header, so throttling is expected earlier than with `PRO` or `ENTERPRISE`.
- The benchmark script’s `SCALE` argument is only a reporting label in CSV; scaling is controlled separately by `docker compose --scale`.
- Because the gateway uses local buckets plus Redis leasing, benchmark results should be interpreted as both throughput data and distributed-consistency behavior under contention.
