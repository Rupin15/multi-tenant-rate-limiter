package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.services;

import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.repository.RateLimitPolicy;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.repository.TenantTier;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;

@Service
@RequiredArgsConstructor
public class RateLimitPolicyResolver {
    public static final String TENANT_TIER_ATTRIBUTE = "tenantTier";
    private final RateLimitPolicyRegistry registry;

    public ResolvedRateLimitPolicy resolve(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String routeId = route == null ? "default" : route.getId();

        TenantTier tier = exchange.getAttribute(TENANT_TIER_ATTRIBUTE);
        if (tier == null) {
            tier = extractTierFallback(exchange);
        }

        RateLimitPolicy policy = registry.getPolicy(routeId, tier);
        return new ResolvedRateLimitPolicy(routeId, tier, policy);
    }

    private TenantTier extractTierFallback(ServerWebExchange exchange) {
        String headerTier = exchange.getRequest().getHeaders().getFirst("X-Tenant-Tier");
        if (headerTier != null && !headerTier.isBlank()) {
            return TenantTier.from(headerTier);
        }

        String queryTier = exchange.getRequest().getQueryParams().getFirst("tier");
        if (queryTier != null && !queryTier.isBlank()) {
            return TenantTier.from(queryTier);
        }

        return TenantTier.FREE;
    }

    public record ResolvedRateLimitPolicy(String routeId, TenantTier tier, RateLimitPolicy policy) {
    }
}
