package com.springboot.multi_tenant_rate_limiter.configuration_server.policy;

public record RateLimitPolicy(
        String routeId,
        TenantTier tier,
        String name,
        long maxTokens,
        long refillTokensPerSecond,
        long leaseSize,
        long version
) {
}
