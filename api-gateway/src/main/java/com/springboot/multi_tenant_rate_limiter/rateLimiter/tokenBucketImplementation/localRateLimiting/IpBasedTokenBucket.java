package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.localRateLimiting;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class IpBasedTokenBucket {

    private final ConcurrentHashMap<String, TokenBucket> bucketsByIp =
            new ConcurrentHashMap<>();

    private final MeterRegistry meterRegistry;

    public IpBasedTokenBucket(MeterRegistry meterRegistry) {

        this.meterRegistry = meterRegistry;

        Gauge.builder(
                        "rate_limiter.local.tracked_ips",
                        bucketsByIp,
                        ConcurrentHashMap::size
                )
                .description("Tracked local token buckets")
                .register(meterRegistry);
    }

    @Timed(
            value = "rate_limiter.local.bucket.lookup.time",
            description = "Time spent retrieving local token buckets"
    )
    @Counted(
            value = "rate_limiter.local.bucket.lookups",
            description = "Local token bucket lookups"
    )
    @Observed(name = "rate_limiter.local.bucket.lookup")
    public TokenBucket getBucket(String ip) {

        return bucketsByIp.computeIfAbsent(
                ip,
                ignored -> new TokenBucket(meterRegistry)
        );
    }

    @Timed(
            value = "rate_limiter.local.cleanup.time",
            description = "Time spent cleaning expired token buckets"
    )
    @Counted(
            value = "rate_limiter.local.cleanup.runs",
            description = "Token bucket cleanup executions"
    )
    @Observed(name = "rate_limiter.local.cleanup")
    public void removeExpiredBuckets(long ttlNanos) {

        long currentTimestamp = System.nanoTime();

        bucketsByIp.entrySet().removeIf(entry -> {

            TokenBucket bucket = entry.getValue();

            if (bucket == null) {
                return true;
            }

            long idleDuration =
                    currentTimestamp - bucket.getLastUpdatedTimestamp();

            return idleDuration > ttlNanos;
        });
    }
}