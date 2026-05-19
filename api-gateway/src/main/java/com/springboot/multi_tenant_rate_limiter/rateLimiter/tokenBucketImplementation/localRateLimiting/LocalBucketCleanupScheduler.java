package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.localRateLimiting;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class LocalBucketCleanupScheduler {

    private static final long BUCKET_TTL_NANOS = 60L * TimeWindowUnit.MINUTE.perUnitNano();

    private final IpBasedTokenBucket ipBasedTokenBucket;

    @Scheduled(fixedRateString = "${rate-limiter.local.cleanup.interval:3600000}")
    @Timed(
            value = "rate_limiter.local.cleanup.scheduler.time",
            description = "Time spent running local bucket cleanup"
    )
    @Counted(
            value = "rate_limiter.local.cleanup.scheduler.runs",
            description = "Local bucket cleanup scheduler executions"
    )
    @Observed(name = "rate_limiter.local.cleanup.scheduler")
    public void cleanupExpiredBuckets() {
        ipBasedTokenBucket.removeExpiredBuckets(BUCKET_TTL_NANOS);
    }
}