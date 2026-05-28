package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.repository;

public record RateLimitPolicy(
        String name,
        long maxTokens,
        long refillTokensPerSecond,
        long leaseSize,
        long version
) {}