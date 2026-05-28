package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.services;

import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.repository.RateLimitPolicy;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.repository.TenantTier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitPolicyRegistryTest {

    @Test
    void shouldFallbackToTierDefaultWhenRouteMissing() {
        RateLimitPolicyRegistry registry = new RateLimitPolicyRegistry();

        RateLimitPolicy policy = registry.getPolicy("missing-route", TenantTier.PRO);

        assertThat(policy.tier()).isEqualTo(TenantTier.PRO);
        assertThat(policy.routeId()).isEqualTo("default");
        assertThat(policy.maxTokens()).isEqualTo(80);
    }

    @Test
    void shouldReplacePoliciesAndApplyVersionGuard() {
        RateLimitPolicyRegistry registry = new RateLimitPolicyRegistry();
        RateLimitPolicy v2 = new RateLimitPolicy("payment-gateway", TenantTier.FREE, "PAYMENTS_FREE", 60, 4, 4, 2);
        RateLimitPolicy v1 = new RateLimitPolicy("payment-gateway", TenantTier.FREE, "PAYMENTS_FREE_OLD", 10, 1, 1, 1);

        registry.replacePolicies(List.of(v2));
        registry.refreshPolicy("payment-gateway", TenantTier.FREE, v1);

        RateLimitPolicy policy = registry.getPolicy("payment-gateway", TenantTier.FREE);
        assertThat(policy.version()).isEqualTo(2);
        assertThat(policy.maxTokens()).isEqualTo(60);
    }
}
