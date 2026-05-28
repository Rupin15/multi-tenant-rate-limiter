# Multi-Tenant Rate Limiter

## Overview

This repository is a multi-module Spring Boot backend system for distributed, tier-aware rate limiting.  
The gateway combines local token buckets with Redis-backed Lua leases and receives its policy configuration from a dedicated configuration server.

## Modules

- `api-gateway`
  - Spring Cloud Gateway (WebFlux)
  - Tier-aware rate limiting (`FREE`, `PRO`, `ENTERPRISE`)
  - Pull + pub/sub policy sync from configuration server
  - Chaos injection endpoint: `POST /admin/chaos`
  - Prometheus metrics + OpenTelemetry traces + correlation ID propagation

- `configuration-server`
  - Source of truth for rate-limit policies
  - Persists tiered route policies in Postgres
  - Uses outbox + Redis pub/sub for policy update propagation
  - Admin APIs:
    - `GET /admin/policies`
    - `POST /admin/policies`
  - Internal policy feed:
    - `GET /internal/config/rate-limit-policies`

- `payment-service`
  - Downstream service (`GET /v1/payments/healthcheck`)
  - Correlation ID + OpenTelemetry enabled

- `order-service`
  - Downstream service (`GET /v1/orders/healthcheck`)
  - Correlation ID + OpenTelemetry enabled

## Infrastructure

- `redis` for distributed leases + policy pub/sub
- `postgres-policy` for policy and outbox persistence
- `prometheus` for scraping metrics
- `grafana` for dashboards
- `jaeger` for distributed tracing

## Architecture Flow

1. Request enters `api-gateway`.
2. Gateway resolves tenant tier from:
   - `X-Tenant-Tier` header, else
   - `tier` query param, else
   - JSON body field `tier`, else defaults to `FREE`.
3. Gateway resolves route+tier policy from local cache.
4. Local token bucket check runs first.
5. If local tokens are exhausted, Redis Lua lease is requested.
6. Policies are synced from `configuration-server` and updated via Redis events.

## Run

From repo root:

```bash
docker-compose up
```

Gateway routes:

- `/api/payments/**` -> `payment-service`
- `/api/orders/**` -> `order-service`

## Key Config Files

- Gateway config: `api-gateway/src/main/resources/application.yml`
- Config server config: `configuration-server/src/main/resources/application.yml`
- DB schema/seed: `postgres/init/01-schema.sql`
- Prometheus config: `monitoring/prometheus.yml`
- Grafana dashboard: `monitoring/grafana/dashboards/rate-limiter-dashboard.json`

## Validation

Run compile + tests:

```bash
mvn test
```
