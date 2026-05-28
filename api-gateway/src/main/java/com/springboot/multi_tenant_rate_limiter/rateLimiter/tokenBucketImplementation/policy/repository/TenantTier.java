package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.repository;

public enum TenantTier {
    FREE,
    PRO,
    ENTERPRISE;

    public static TenantTier from(String rawTier) {
        if (rawTier == null || rawTier.isBlank()) {
            return FREE;
        }
        try {
            return TenantTier.valueOf(rawTier.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return FREE;
        }
    }
}
