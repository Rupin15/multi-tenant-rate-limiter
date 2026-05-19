package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy;

public record RateLimitPolicy(
        String name,
        long maxTokens,
        long refillTokensPerSecond,
        long leaseSize
) {}