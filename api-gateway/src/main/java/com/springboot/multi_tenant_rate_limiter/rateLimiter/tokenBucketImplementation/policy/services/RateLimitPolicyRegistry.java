package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.services;

import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.repository.RateLimitPolicy;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.repository.TenantTier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitPolicyRegistry {

    private static final String DEFAULT_ROUTE_ID = "default";
    private final Map<String, RateLimitPolicy> policyRegistry = new ConcurrentHashMap<>();
    private final Map<TenantTier, RateLimitPolicy> defaultPolicies = Map.of(
            TenantTier.FREE, new RateLimitPolicy(DEFAULT_ROUTE_ID, TenantTier.FREE, "DEFAULT_FREE", 20, 2, 1, 1),
            TenantTier.PRO, new RateLimitPolicy(DEFAULT_ROUTE_ID, TenantTier.PRO, "DEFAULT_PRO", 80, 8, 5, 1),
            TenantTier.ENTERPRISE, new RateLimitPolicy(DEFAULT_ROUTE_ID, TenantTier.ENTERPRISE, "DEFAULT_ENTERPRISE", 200, 20, 10, 1)
    );

    public RateLimitPolicyRegistry() {
        defaultPolicies.values().forEach(policy -> policyRegistry.put(policyKey(policy.routeId(), policy.tier()), policy));
    }

    public RateLimitPolicy getPolicy(String routeId, TenantTier tier) {
        String effectiveRouteId = routeId == null || routeId.isBlank() ? DEFAULT_ROUTE_ID : routeId;
        String routeKey = policyKey(effectiveRouteId, tier);
        RateLimitPolicy routePolicy = policyRegistry.get(routeKey);
        if (routePolicy != null) {
            return routePolicy;
        }
        return defaultPolicies.getOrDefault(tier, defaultPolicies.get(TenantTier.FREE));
    }

    public synchronized void replacePolicies(List<RateLimitPolicy> policies) {
        policyRegistry.clear();
        defaultPolicies.values().forEach(policy -> policyRegistry.put(policyKey(policy.routeId(), policy.tier()), policy));
        for (RateLimitPolicy policy : policies) {
            refreshPolicy(policy.routeId(), policy.tier(), policy);
        }
    }

    public void refreshPolicy(String routeId, TenantTier tier, RateLimitPolicy incomingPolicy) {
        String key = policyKey(routeId, tier);
        RateLimitPolicy existing = policyRegistry.get(key);
        if (existing == null || incomingPolicy.version() >= existing.version()) {
            policyRegistry.put(key, incomingPolicy);
        }
    }

    private String policyKey(String routeId, TenantTier tier) {
        return routeId + "::" + tier.name();
    }
}
