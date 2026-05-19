package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.localRateLimiting;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {
    private long ttlSeconds = 7200;
    public long getTtlSeconds() {
        return ttlSeconds;
    }
}
