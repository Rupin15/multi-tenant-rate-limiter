package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.localRateLimiting;

import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.RateLimitPolicy;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class IpBasedTokenBucket {

    private final ConcurrentHashMap<String, TokenBucket> buckets =
            new ConcurrentHashMap<>();

    public IpBasedTokenBucket(
            MeterRegistry meterRegistry
    ) {

        Gauge.builder(
                        "rate_limiter.local.tracked_buckets",
                        buckets,
                        ConcurrentHashMap::size
                )
                .register(meterRegistry);
    }

    public TokenBucket getBucket(
            String ip,
            RateLimitPolicy policy
    ) {

        String key =
                ip + ":" + policy.name();
        return buckets.computeIfAbsent(
                key,
                ignored -> new TokenBucket(
                        policy.getLeaseSize(),
                        null
                )
        );
    }

    public void removeExpiredBuckets(long ttlNanos) {
        long currentTime = System.nanoTime();
        buckets.entrySet().removeIf(entry -> {
            TokenBucket bucket = entry.getValue();
            return currentTime
                    - bucket.getLastUpdatedTimestamp()
                    > ttlNanos;
        });
    }
}