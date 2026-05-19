package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.localRateLimiting;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {
    private long maxTokens = 20;
    private long refillTokensPerMilliSecond = 0;
    private long leaseSize = 10;
    private long ttlSeconds = 7200;

    public long getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(long maxTokens) {
        this.maxTokens = maxTokens;
    }

    public long getRefillTokensPerMilliSecond() {
        return refillTokensPerMilliSecond;
    }

    public void setRefillTokensPerMilliSecond(long refillTokensPerSecond) {
        this.refillTokensPerMilliSecond = refillTokensPerSecond;
    }

    public long getLeaseSize() {
        return leaseSize;
    }

    public void setLeaseSize(long leaseSize) {
        this.leaseSize = leaseSize;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }
}
