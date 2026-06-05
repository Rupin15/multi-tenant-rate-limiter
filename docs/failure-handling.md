# Failure Handling

## Design goal

The system is designed to avoid turning a dependency failure into a total request outage. In most cases it prefers controlled degradation, stale-but-valid configuration, or fallback responses over complete unavailability.

## Failure scenarios

## Redis lease allocation failure

Where it happens:

- `api-gateway` when the local bucket is empty and a Redis lease is needed.

Handling:

- Redis lease calls are wrapped with Resilience4j bulkhead, time limiter, retry, and circuit breaker.
- If Redis is already marked unhealthy, the gateway skips the Redis lease request entirely.
- The gateway falls back to `tryConsumeWithLocalRefill()` on the in-memory bucket.
- A Redis fallback metric is emitted.

Impact:

- The gateway continues serving traffic.
- Rate limiting becomes instance-local instead of globally coordinated until Redis recovers.
- Strict distributed fairness is weakened, but traffic is still bounded locally.

## Redis health degradation

Where it is detected:

- `RedisHealthMonitor`
- `RedisHealthCheckScheduler`

Handling:

- Scheduled Redis ping checks update shared `RedisHealthState`.
- When Redis is healthy again, the state flips back to healthy.
- When healthy, the gateway attempts to restart the Redis pub/sub listener manager if needed.

Impact:

- The health state controls whether the gateway uses the normal Redis-backed flow or degraded local mode.

## Redis pub/sub event processing failure

Where it happens:

- `RedisPolicySubscriber`

Handling:

- Duplicate events are ignored using an in-memory processed-event set keyed by event ID.
- Event processing exceptions mark Redis unhealthy.
- The subscriber does not become the only source of truth because the gateway also performs periodic full sync.
- Processed event IDs are cleaned up on a schedule to prevent unbounded memory growth.

Impact:

- A specific policy event may be missed or delayed locally.
- The next periodic pull sync from the configuration server reconciles the cache.

## Configuration server unavailable during gateway sync

Where it happens:

- `PolicyConfigSyncScheduler`

Handling:

- Startup and scheduled sync attempts catch exceptions and record a failure metric.
- The gateway keeps the existing in-memory registry unchanged.
- If the gateway has not synced yet, it still has built-in default policies for `FREE`, `PRO`, and `ENTERPRISE`.

Impact:

- The system keeps using the last known policy snapshot.
- Policy propagation latency increases until the configuration server becomes available again.

## Chaos-forced config sync failure

Where it happens:

- `ChaosInjectionService`
- `PolicyConfigSyncScheduler`

Handling:

- When the chaos flag is enabled, periodic sync is intentionally skipped.
- A distinct `chaos_failure` metric is recorded.

Impact:

- Used to validate stale-cache tolerance and operational observability.

## Policy publication failure after database commit

Where it happens:

- `ImmediatePolicyPublisher`

Handling:

- Policy changes are stored in the outbox table in the same transaction as the policy update.
- After commit, the immediate publisher tries Redis pub/sub.
- If Redis is unavailable or publication fails, the outbox row stays unprocessed.
- `PolicyOutboxPublisher` retries unprocessed rows on a schedule.

Impact:

- Policy durability is preserved even if Redis is down at update time.
- Publication becomes delayed instead of lost.

## Concurrent policy updates

Where it happens:

- `configuration-server` JPA update path

Handling:

- `RateLimitPolicyEntity` uses JPA optimistic locking through `@Version`.
- `RateLimitPolicyService` catches `ObjectOptimisticLockingFailureException`.
- The service throws a retryable application-level error message: `Policy update conflict detected. Please retry.`

Impact:

- Prevents silent last-write-wins behavior for conflicting updates.

## Downstream service failure

Where it happens:

- Gateway routes `payment-gateway` and `order-gateway`

Handling:

- Resilience4j circuit breakers are configured per route.
- Failures forward to `/fallback/payment` or `/fallback/order`.
- Fallback endpoints return `503 Service Unavailable`.

Impact:

- Users receive a controlled failure response from the gateway rather than raw downstream connection errors.

## Malformed or missing tenant tier

Where it happens:

- `TenantTierExtractionFilter`
- `RateLimitPolicyResolver`

Handling:

- Header, query parameter, and JSON body extraction are attempted.
- If the body is missing or parsing fails, the gateway defaults to `FREE`.
- If no route-specific policy exists, the registry falls back to the default policy for that tier.

Impact:

- The request is still processed.
- The safest default is applied instead of failing the request during classification.

## Local bucket state contention

Where it happens:

- `TokenBucket`

Handling:

- Token state updates use compare-and-set retries.
- If retries are exhausted, a metric is emitted and the operation returns `false`.

Impact:

- Under extreme contention, some requests may be rejected conservatively instead of allowing inconsistent bucket state.

## Operational cleanup and bounded state

Local memory protections:

- Expired local IP buckets are removed by `LocalBucketCleanupScheduler`.
- Processed Redis event IDs are purged by `ProcessedEventCleanupScheduler`.

Impact:

- Reduces long-lived memory growth in steady-state operation.

## Failure-handling summary

| Failure | Primary response | Secondary safety net |
| --- | --- | --- |
| Redis lease failure | Degraded local refill mode | Redis health recovery checks |
| Redis pub/sub failure | Keep current cache | Periodic full sync from config server |
| Config server sync failure | Keep last known policies | Built-in default tier policies |
| Policy publish failure | Persist outbox row | Scheduled outbox republisher |
| Concurrent policy update | Optimistic lock conflict | Client retries |
| Downstream service failure | Gateway fallback endpoint | Circuit breaker recovery |

## Important current limitations

- Degraded local mode preserves availability but weakens cluster-wide enforcement accuracy.
- Processed Redis event deduplication is in-memory only and resets on restart.
- Admin and internal endpoints are not protected by authentication in the current code.
- There is no dead-letter queue for permanently failing outbox publications.
