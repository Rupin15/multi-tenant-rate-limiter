# Distributed Tier-Aware Rate-limiting Gateway

This repository contains a distributed, tier-aware rate limiting system built with Spring Boot. It combines a reactive API gateway, Redis-backed lease coordination, a configuration service backed by Postgres, and operational tooling for tracing, metrics, dashboards, and load testing.

## What the system does

- Applies route-specific and tenant-tier-specific rate limits at the gateway.
- Uses a hybrid token bucket design:
  local in-memory buckets for fast admission checks, then Redis Lua leasing for distributed coordination.
- Stores policy definitions in Postgres and publishes policy changes through Redis pub/sub.
- Keeps gateway instances consistent with both periodic pull sync and event-driven cache refresh.
- Exposes metrics, traces, health endpoints, and benchmark tooling for operational visibility.

## Repository structure

| Module | Role | Main stack |
| --- | --- | --- |
| `api-gateway` | Entry point, routing, rate limiting, fallbacks, chaos toggles | Spring Cloud Gateway, WebFlux, Redis, Resilience4j, Micrometer |
| `configuration-server` | Source of truth for rate-limit policies | Spring MVC, JPA, Postgres, Redis pub/sub, outbox pattern |
| `payment-service` | Downstream sample service | Spring MVC, Actuator, tracing |
| `order-service` | Downstream sample service | Spring MVC, Actuator, tracing |
| `postgres/init` | Schema and seed policy data | PostgreSQL SQL |
| `monitoring` | Prometheus and Grafana setup | Prometheus, Grafana |
| `nginx` | Front-door reverse proxy to the gateway | NGINX |
| `benchmark` | Load generation and benchmark result capture | k6, shell, batch |

## Architecture at a glance

1. A client calls NGINX on `:8084`.
2. NGINX forwards the request to `api-gateway`.
3. The gateway attaches or propagates `X-Correlation-Id`.
4. The gateway resolves tenant tier from header, query parameter, or JSON body.
5. The gateway resolves the matching route policy from its in-memory registry.
6. A local token bucket is checked first for fast admission.
7. If the local bucket is empty, the gateway requests a Redis lease through a Lua script.
8. If Redis is unhealthy, the gateway falls back to degraded local refill mode.
9. If the request is allowed, the gateway forwards to `payment-service` or `order-service`.
10. If a downstream service is failing, the gateway circuit breaker routes to a fallback endpoint.

Policy updates flow separately:

1. An admin updates a policy through `configuration-server`.
2. The policy is persisted in Postgres.
3. A policy event is written to the outbox in the same transaction.
4. After commit, the event is published to Redis pub/sub when possible.
5. Gateway instances refresh their local caches from the event.
6. If pub/sub misses an update, periodic sync from the configuration server reconciles the cache.

## Backend concepts implemented

- API Gateway pattern
- Reactive request processing with Spring WebFlux
- Token bucket rate limiting
- Hybrid local plus distributed coordination
- Redis Lua scripting for atomic lease allocation
- Policy cache with version-based refresh protection
- Outbox pattern for durable event publication
- Event-driven cache invalidation with Redis pub/sub
- Scheduled reconciliation sync
- Circuit breaker, retry, bulkhead, and time limiter with Resilience4j
- Correlation ID propagation and OpenTelemetry tracing
- Metrics and health endpoints with Micrometer and Actuator
- Optimistic locking for concurrent policy updates
- Chaos toggles for resilience testing

## Documentation

- [Architecture](docs/architecture.md)
- [Failure Handling](docs/failure-handling.md)
- [Backend Concepts](docs/backend-concepts.md)
- [Runbook](docs/runbook.md)
- [Benchmarking](docs/benchmarking.md)

## Key endpoints

Gateway routes:

- `GET http://localhost:8084/api/payments/healthcheck`
- `GET http://localhost:8084/api/orders/healthcheck`

Gateway operational endpoints:

- `GET http://localhost:8084/healthcheck`
- `GET http://localhost:8084/fallback/payment`
- `GET http://localhost:8084/fallback/order`
- `GET http://localhost:8084/admin/chaos`
- `POST http://localhost:8084/admin/chaos`
- `GET http://localhost:9091/actuator/health` from inside the `api-gateway` container or when running the gateway locally
- `GET http://localhost:9091/actuator/prometheus` from inside the `api-gateway` container or when running the gateway locally

Configuration server endpoints:

- `GET http://localhost:8085/internal/config/rate-limit-policies`
- `GET http://localhost:8085/admin/policies`
- `POST http://localhost:8085/admin/policies`
- `GET http://localhost:9094/actuator/health`
- `GET http://localhost:9094/actuator/prometheus`

Operational UIs:

- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`
- Jaeger: `http://localhost:16686`

## Quick start

Run the full stack:

```bash
docker compose up --build
```

Then verify:

```bash
curl -i http://localhost:8084/api/payments/healthcheck
curl -i http://localhost:8084/api/orders/healthcheck
curl -s http://localhost:8085/admin/policies
curl -s http://localhost:8084/healthcheck
```

Run tests from the repository root:

```bash
mvn test
```

## Security posture in the current code

- Spring Security is present in all services.
- All services currently permit all requests.
- CSRF is disabled in the gateway and configuration server.
- There is no authentication or authorization layer implemented for admin endpoints yet.

## Notes for operators

- Redis is used for distributed token lease allocation and policy pub/sub.
- Postgres is the persistence layer for policies and the outbox.
- Default policies are hard-coded in the gateway registry and also seeded in Postgres.
- If Redis or config sync is unavailable, the system prefers continued availability with degraded behavior rather than hard failure.

For setup details, operational procedures, testing, and troubleshooting, use the [Runbook](docs/runbook.md).
