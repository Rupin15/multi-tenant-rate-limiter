# Backend Concepts

This document maps the backend engineering concepts in the project to their concrete implementation.

## API Gateway pattern

Implemented in `api-gateway` using Spring Cloud Gateway.

Why it is used:

- Centralizes routing.
- Applies cross-cutting concerns before traffic reaches services.
- Keeps rate limiting, resilience, and observability in one entry point.

Concrete use in this project:

- `/api/payments/**` routes to `payment-service`
- `/api/orders/**` routes to `order-service`
- Circuit breaker filters are attached at the route level.

## Reactive backend processing

Implemented in `api-gateway` using Spring WebFlux and Reactor.

Why it matters:

- Gateway operations are I/O heavy.
- Redis lease requests and gateway filter chains benefit from non-blocking execution.

Concrete use:

- Global filters return `Mono<Void>`.
- Redis lease calls use `ReactiveRedisTemplate`.
- Downstream fallback endpoints return `Mono<ResponseEntity<String>>`.

## Token bucket algorithm

Implemented in two places:

- Local in-memory bucket: `TokenBucket`
- Distributed lease bucket: Redis Lua script `tokenBucket.lua`

Concept:

- A bucket has a token capacity, refill rate, and token consumption per request.
- If at least one token exists, the request is allowed.
- If not, the request is rejected or another lease source is consulted.

Why this project uses it:

- It provides smooth rate limiting.
- It supports burst allowance up to `maxTokens`.
- It is efficient for high-throughput admission control.

## Hybrid local plus distributed rate limiting

Implemented by combining `TokenBucket` and `LuaRateLimiterService`.

Concept:

- Local buckets serve as a near-cache of admission tokens.
- Redis provides distributed lease allocation to coordinate across instances.

Why this project uses it:

- Pure local limiting is fast but not globally accurate.
- Pure Redis-per-request limiting is accurate but adds more network overhead.
- The hybrid model balances latency, throughput, and cross-node coordination.

## Redis Lua scripting

Implemented in `api-gateway/src/main/resources/script/tokenBucket.lua`.

Concept:

- Lua scripts run atomically inside Redis.
- The script refills the bucket, applies leasing logic, updates the hash, and sets TTL in one execution.

Why it is used:

- Prevents race conditions across multiple gateway instances.
- Keeps refill and lease logic consistent at the distributed coordination layer.

## Policy registry and cache

Implemented in `RateLimitPolicyRegistry`.

Concept:

- Gateways keep policy data in memory for fast reads.
- The registry is keyed by `routeId + tier`.
- Default policies are always present.

Why it is used:

- Rate limiting cannot afford a database or HTTP lookup on every request.
- The registry provides fast local reads with eventual consistency.

## Version-based state protection

Implemented in `RateLimitPolicyRegistry.refreshPolicy`.

Concept:

- Only update a cached policy if the incoming version is newer or equal.

Why it is used:

- Prevents stale pub/sub events from overwriting fresher policy state.

## Pull plus push configuration propagation

Implemented through:

- `PolicyConfigSyncScheduler` for pull
- `RedisPolicySubscriber` for push

Concept:

- Push gives low-latency updates.
- Pull provides eventual reconciliation and recovery from missed events.

Why it is used:

- Pub/sub alone is not durable enough.
- Polling alone is slower than needed for operational updates.
- Together they create a practical eventual-consistency model.

## Transactional outbox pattern

Implemented in `configuration-server`.

Core pieces:

- `RateLimitPolicyService`
- `PolicyEventOutboxEntity`
- `ImmediatePolicyPublisher`
- `PolicyOutboxPublisher`

Concept:

- Persist the business change and the event in the same transaction.
- Publish the event asynchronously after commit.
- Retry later if publication fails.

Why it is used:

- Prevents the classic inconsistency where the database commit succeeds but the event is lost.

## Optimistic locking

Implemented by `RateLimitPolicyEntity` with JPA `@Version`.

Concept:

- Concurrent writers do not block each other pessimistically.
- Conflicts are detected when a stale version attempts to commit.

Why it is used:

- Appropriate for administrative policy updates that should detect conflicts explicitly.

## Resilience patterns

Implemented in the gateway around Redis lease allocation and downstream routing.

Patterns used:

- Circuit breaker
- Retry
- Time limiter
- Bulkhead
- Fallback endpoint

Why they are used:

- Prevent dependency slowdown from cascading.
- Limit concurrency against a stressed dependency.
- Bound latency.
- Recover automatically when dependencies become healthy.

## Health-driven degradation

Implemented through:

- `RedisHealthState`
- `RedisHealthMonitor`
- `RedisHealthCheckScheduler`

Concept:

- Health checks update a shared state flag.
- Request handling reads that flag to decide whether to use normal Redis-backed behavior or degraded local mode.

Why it is used:

- Avoids repeatedly attempting expensive failing calls when Redis is already known unhealthy.

## Correlation ID propagation

Implemented in all services.

Concept:

- Every request gets an `X-Correlation-Id`.
- The value is propagated to downstream services and written to logs.

Why it is used:

- Makes multi-service request tracing easier even without searching by trace ID first.

## Distributed tracing

Implemented with Micrometer tracing and OTLP export to Jaeger.

Concept:

- Traces connect gateway and downstream service spans into one request path.

Why it is used:

- Useful for latency diagnosis and resilience testing.

## Metrics and observability

Implemented with Spring Boot Actuator and Prometheus.

Concepts used:

- counters for outcomes and failures
- timers for execution duration
- gauges for live state
- exposed scrape endpoints for Prometheus

Why it is used:

- Gives direct visibility into rate-limit decisions, degraded mode frequency, sync outcomes, and dependency health.

## Scheduling

Implemented with Spring scheduling annotations.

Scheduled jobs:

- policy sync
- Redis health checks
- outbox retries
- local bucket cleanup
- processed event cleanup

Why it is used:

- Keeps eventually consistent components repaired without manual intervention.

## Validation and defensive defaults

Implemented with:

- request validation on policy updates using Jakarta Validation
- default tenant fallback to `FREE`
- default policy fallback by tier

Why it is used:

- Keeps the system operating safely when inputs are incomplete or invalid.

## Security baseline

Implemented with Spring Security, but currently configured to permit all requests.

Why this matters:

- Security middleware is present structurally.
- Authentication and authorization are not yet enforced.
- Admin endpoints are operationally useful for development and testing, but they are not production-hardened in the current state.
