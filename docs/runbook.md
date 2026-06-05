# Runbook

## Purpose

This document explains how to build, run, verify, operate, and troubleshoot the full stack.

## Prerequisites

For Docker-based execution:

- Docker
- Docker Compose support

For local Maven execution:

- Java 25
- Maven 3.9+
- Redis
- Postgres 16

Optional tooling:

- `curl`
- `jq`
- `k6`

## Run the full stack with Docker

From the repository root:

```bash
docker compose up --build
```

What starts:

- `redis`
- `postgres-policy`
- `configuration-server`
- `payment-service`
- `order-service`
- `api-gateway`
- `nginx`
- `prometheus`
- `grafana`
- `jaeger`

## Exposed ports

| Service | Port | Purpose |
| --- | --- | --- |
| NGINX | `8084` | External entry point |
| Configuration server | `8085` | Admin and internal policy APIs |
| Configuration server actuator | `9094` | Config service health and metrics |
| Prometheus | `9090` | Metrics UI and query endpoint |
| Grafana | `3000` | Dashboards |
| Jaeger | `16686` | Trace UI |
| Redis | `6379` | Redis access |
| Postgres | `5432` | Postgres access |

Notes:

- `payment-service`, `order-service`, and `api-gateway` expose internal `8080` ports inside Docker networking.
- `api-gateway` actuator port `9091` is not published to the host in `docker-compose.yml`.
- NGINX is the intended front door for functional testing from the host.

## Build and test

Run all tests:

```bash
mvn test
```

Build all modules:

```bash
mvn clean package
```

## Basic verification

### Verify health

```bash
curl -s http://localhost:8084/healthcheck
curl -s http://localhost:9094/actuator/health
curl -s http://localhost:8085/admin/policies
```

If you need the gateway actuator from the Docker deployment:

```bash
docker compose exec api-gateway curl -s http://localhost:9091/actuator/health
docker compose exec api-gateway curl -s http://localhost:9091/actuator/prometheus
```

### Verify routing

```bash
curl -i http://localhost:8084/api/payments/healthcheck
curl -i http://localhost:8084/api/orders/healthcheck
```

Expected:

- `200 OK`
- response body from downstream service
- `X-Correlation-Id` header present

### Verify tier-aware requests

```bash
curl -i -H "X-Tenant-Tier: FREE" http://localhost:8084/api/payments/healthcheck
curl -i -H "X-Tenant-Tier: PRO" http://localhost:8084/api/payments/healthcheck
curl -i -H "X-Tenant-Tier: ENTERPRISE" http://localhost:8084/api/payments/healthcheck
```

### Observe rate limiting

Send repeated requests quickly:

```bash
for i in {1..100}; do
  curl -s -o /dev/null -w "%{http_code}\n" \
    -H "X-Tenant-Tier: FREE" \
    http://localhost:8084/api/payments/healthcheck
done
```

Expected:

- a mix of `200` and `429` depending on request rate and bucket state

## Policy operations

### List policies

```bash
curl -s http://localhost:8085/admin/policies | jq
```

### Update or create a policy

```bash
curl -s -X POST http://localhost:8085/admin/policies \
  -H "Content-Type: application/json" \
  -d '{
    "routeId": "payment-gateway",
    "tier": "FREE",
    "name": "PAYMENTS_FREE",
    "maxTokens": 10,
    "refillTokensPerSecond": 1,
    "leaseSize": 1
  }' | jq
```

What should happen:

- the policy is persisted in Postgres
- an outbox event is created
- Redis pub/sub should notify gateway instances
- periodic sync will also converge state if pub/sub is missed

## Chaos testing

### Read current chaos state

```bash
curl -s http://localhost:8084/admin/chaos
```

### Force Redis lease failures and config sync failures

```bash
curl -s -X POST http://localhost:8084/admin/chaos \
  -H "Content-Type: application/json" \
  -d '{
    "redisLeaseFailureEnabled": true,
    "configSyncFailureEnabled": true
  }'
```

Usage:

- enable Redis lease failure to validate degraded local mode
- enable config sync failure to validate stale-cache behavior and metrics

Reset:

```bash
curl -s -X POST http://localhost:8084/admin/chaos \
  -H "Content-Type: application/json" \
  -d '{
    "redisLeaseFailureEnabled": false,
    "configSyncFailureEnabled": false
  }'
```

## Benchmarking

Linux or macOS:

```bash
cd benchmark
./benchmark.sh 5 500
```

Windows:

```bat
cd benchmark
benchmark.bat 5 500
```

Direct k6 execution:

```bash
k6 run -e RATE=500 benchmark/load-test.js
```

Benchmark target:

- `http://localhost:8084/api/payments/healthcheck`

Benchmark expectations:

- successful requests can be `200`
- throttled requests can be `429`

## Observability

### Prometheus

- URL: `http://localhost:9090`
- Scrapes gateway, configuration server, payment service, and order service.

### Grafana

- URL: `http://localhost:3000`
- Default credentials:
  `admin / admin`

Provisioned content:

- rate limiter dashboard
- benchmark dashboard

### Jaeger

- URL: `http://localhost:16686`
- Use it to trace request flow across gateway and downstream services.

## Run services locally without Docker

## 1. Start dependencies

Start Redis and Postgres locally, or use Docker only for infra:

```bash
docker compose up redis postgres-policy
```

## 2. Export required environment variables

Configuration server:

```bash
export SPRING_DATA_REDIS_HOST=localhost
export SPRING_DATA_REDIS_PORT=6379
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/rate_limiter?options=-c%20TimeZone=UTC"
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
```

Gateway:

```bash
export SPRING_DATA_REDIS_HOST=localhost
export SPRING_DATA_REDIS_PORT=6379
export RATE_LIMITER_CONFIG_SERVER_BASE_URL=http://localhost:8085/internal/config/rate-limit-policies
export SPRING_PAYMENT_URL=http://localhost:8081
export SPRING_ORDER_URL=http://localhost:8082
```

Optional tracing export:

```bash
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318/v1/traces
```

## 3. Run each module

Configuration server:

```bash
mvn -pl configuration-server spring-boot:run
```

Payment service:

```bash
mvn -pl payment-service spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

Order service:

```bash
mvn -pl order-service spring-boot:run -Dspring-boot.run.arguments=--server.port=8082
```

Gateway:

```bash
mvn -pl api-gateway spring-boot:run
```

Notes:

- the gateway expects payment service at `:8081` and order service at `:8082` outside Docker
- actuator ports remain `9091` for gateway and `9091` for the sample services unless overridden, so run them one at a time locally or override management ports as well

## Troubleshooting

## Gateway returns `429`

Likely causes:

- expected rate limiting under load
- `FREE` tier limits are intentionally low
- repeated requests from the same IP and tier

Actions:

- retry after a short delay
- test with another tier such as `PRO`
- inspect gateway metrics in Prometheus or Grafana

## Gateway returns `503`

Likely causes:

- downstream service is unavailable
- route circuit breaker is open

Actions:

- call downstream actuator or health endpoint directly
- inspect gateway logs and circuit breaker actuator endpoints

## Policy changes do not appear immediately

Likely causes:

- Redis pub/sub issue
- configuration server could not publish immediately

Actions:

- inspect configuration server logs
- verify Redis health
- wait for periodic sync to reconcile
- check outbox publish metrics

## Redis unavailable

Expected behavior:

- gateway should continue in degraded local refill mode

Actions:

- verify Redis container health
- inspect gateway Redis health metrics
- disable chaos flags if enabled

## Postgres startup problems

Actions:

- verify `postgres-policy` container health
- confirm database name `rate_limiter`
- confirm schema initialization completed

## Shutdown

Stop the stack:

```bash
docker compose down
```

Stop and remove volumes:

```bash
docker compose down -v
```

Use `-v` only when you want to discard persisted Postgres data.
