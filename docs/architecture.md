# Architecture

## Purpose

This system demonstrates distributed, tenant-aware rate limiting for backend APIs. The implementation is centered on a Spring Cloud Gateway instance that enforces policies before forwarding requests to backend services.

## Components

### `api-gateway`

Responsibilities:

- Accepts all external traffic through NGINX.
- Resolves tenant tier.
- Applies route-aware and tier-aware rate limiting.
- Routes requests to downstream services.
- Exposes fallback endpoints for circuit breaker failures.
- Synchronizes policy configuration from the configuration server.
- Publishes metrics and traces.

Key implementation points:

- Spring Cloud Gateway with WebFlux.
- Global filters for correlation ID, tier extraction, and rate limiting.
- Local token buckets keyed by `ip + policy`.
- Redis Lua lease allocation for distributed coordination.
- Resilience4j protection around Redis lease calls.

### `configuration-server`

Responsibilities:

- Stores the source-of-truth rate-limit policies.
- Exposes admin APIs for policy listing and updates.
- Exposes an internal read API for gateway sync.
- Writes policy update events to an outbox.
- Publishes events to Redis pub/sub after commit.

Key implementation points:

- Spring MVC plus JPA.
- Postgres persistence.
- Composite key on `route_id + tier`.
- Optimistic locking with JPA `@Version`.
- Transactional outbox for durable publication.

### `payment-service` and `order-service`

Responsibilities:

- Act as downstream services behind the gateway.
- Provide simple health-check endpoints.
- Participate in trace and correlation ID propagation.

### Redis

Responsibilities:

- Atomic token lease allocation through Lua.
- Policy update pub/sub distribution.
- Health signal for degraded-mode switching.

### Postgres

Responsibilities:

- Persists rate limit policies.
- Persists pending and processed outbox rows.
- Seeds default route and tier policies during initialization.

### Monitoring stack

- Prometheus scrapes actuator Prometheus endpoints.
- Grafana provisions dashboards.
- Jaeger receives OTLP traces.

## Request flow

### 1. Entry

- Client calls `http://localhost:8084/...`.
- NGINX forwards to `api-gateway`.
- NGINX passes `X-Real-IP` and `X-Forwarded-For`, which the gateway uses to identify the client IP.

### 2. Correlation

- The gateway sets `X-Correlation-Id` if missing.
- The same header is returned in the response and propagated to downstream services.
- Services put the correlation ID into the MDC for log correlation.

### 3. Tenant tier extraction

Resolution order:

1. `X-Tenant-Tier` header
2. `tier` query parameter
3. JSON request body field named `tier`
4. fallback to `FREE`

This is implemented before rate limiting so the correct tenant policy is selected for the request.

### 4. Policy resolution

- The gateway resolves the matched route ID from Spring Cloud Gateway routing metadata.
- `RateLimitPolicyRegistry` returns the route-specific policy for the tenant tier.
- If a route-specific policy is missing, the registry falls back to the tier default policy for the `default` route.

### 5. Rate limiting

The system uses a two-stage token bucket strategy:

1. Local fast path
   - Each IP and policy pair gets an in-memory bucket.
   - Buckets start with `leaseSize` tokens.
   - If the local bucket has capacity, the request is allowed immediately.

2. Distributed coordination
   - When the local bucket is empty, the gateway requests a lease from Redis.
   - Redis executes a Lua script that atomically refills and grants up to `leaseSize` tokens.
   - The granted tokens are added back to the local bucket, and one token is consumed for the current request.

This design reduces Redis round-trips while keeping rate limits coordinated across instances.

### 6. Routing and downstream resilience

- Allowed requests are forwarded to `payment-service` or `order-service`.
- Each route has a circuit breaker.
- If a downstream service is failing, the request is forwarded to a fallback endpoint in the gateway and returns `503 Service Unavailable`.

## Policy distribution flow

### Source of truth

- Policies are stored in Postgres table `rate_limit_policies`.
- A policy is uniquely identified by `route_id` and `tier`.
- Each policy includes:
  `name`, `max_tokens`, `refill_tokens_per_second`, `lease_size`, `version`, `last_updated`.

### Update flow

1. Admin calls `POST /admin/policies`.
2. `RateLimitPolicyService` inserts or updates the policy.
3. The service increments the domain `version`.
4. In the same transaction, an outbox row is persisted in `policy_event_outbox`.
5. After transaction commit, an async listener attempts immediate Redis publication.
6. If immediate publication fails, a scheduled publisher retries pending outbox rows.

### Gateway synchronization

The gateway updates policy cache through two channels:

- Pull sync
  - `PolicyConfigSyncScheduler` fetches all policies from `configuration-server` on startup and on a fixed delay.
- Push updates
  - `RedisPolicySubscriber` listens for Redis pub/sub events and refreshes the local cache immediately.

Version checks prevent older events from overwriting newer policy state.

## Data consistency model

- Postgres is the durable system of record.
- Redis pub/sub is best-effort and low-latency.
- The outbox pattern prevents policy changes from being lost between database commit and event publication.
- Periodic full sync repairs any missed pub/sub messages or subscriber downtime.
- Gateway caches are eventually consistent with the configuration server.

## Observability architecture

### Metrics

Gateway metrics include:

- decision counts by route, tier, backend, and outcome
- Redis fallback counts
- Redis health-check outcomes
- config sync outcomes
- local bucket cleanup counts
- local bucket cardinality gauge

Configuration server metrics include:

- policy update counts by route and tier
- outbox publish outcomes

### Tracing

- All services export traces through OTLP.
- Jaeger is the collector and UI.
- Sampling is configured to `1.0`.

### Logging

- Log patterns include `traceId`, `spanId`, and `correlationId`.
- Downstream services add correlation ID to MDC through servlet filters.

## Deployment shape

Containerized stack from `docker-compose.yml`:

- `nginx`
- `api-gateway`
- `configuration-server`
- `payment-service`
- `order-service`
- `redis`
- `postgres-policy`
- `prometheus`
- `grafana`
- `jaeger`

Health checks are configured for Redis, Postgres, the configuration server, the gateway, and both downstream services.

## Current architectural tradeoffs

- The gateway is optimized for availability. When Redis is unavailable, it continues with degraded local enforcement instead of rejecting all traffic.
- Policy synchronization is eventually consistent, not strongly consistent.
- Security infrastructure exists, but authorization is intentionally not enforced in the current implementation.
- Downstream services are intentionally minimal and mainly exist to exercise gateway routing, tracing, and resilience behavior.
