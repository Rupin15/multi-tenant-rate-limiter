package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy;

public enum RateLimitPolicy {

    PAYMENTS(
            50,
            2,
            5
    ),

    ORDERS(
            100,
            5,
            10
    );

    private final long maxTokens;
    private final long refillTokensPerSecond;
    private final long leaseSize;

    RateLimitPolicy(
            long maxTokens,
            long refillTokensPerSecond,
            long leaseSize
    ) {
        this.maxTokens = maxTokens;
        this.refillTokensPerSecond = refillTokensPerSecond;
        this.leaseSize = leaseSize;
    }

    public long getMaxTokens() {
        return maxTokens;
    }

    public long getRefillTokensPerSecond() {
        return refillTokensPerSecond;
    }

    public long getLeaseSize() {
        return leaseSize;
    }
}