package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.services;

import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.repository.RateLimitPolicy;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.repository.TenantTier;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitPolicyResolverTest {

    @Test
    void shouldResolveTierFromExchangeAttribute() {
        RateLimitPolicyRegistry registry = new RateLimitPolicyRegistry();
        RateLimitPolicy policy = new RateLimitPolicy("order-gateway", TenantTier.ENTERPRISE, "ORDERS_ENT", 500, 40, 20, 5);
        registry.replacePolicies(List.of(policy));

        RateLimitPolicyResolver resolver = new RateLimitPolicyResolver(registry);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders/healthcheck"));
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route("order-gateway"));
        exchange.getAttributes().put(RateLimitPolicyResolver.TENANT_TIER_ATTRIBUTE, TenantTier.ENTERPRISE);

        RateLimitPolicyResolver.ResolvedRateLimitPolicy resolved = resolver.resolve(exchange);

        assertThat(resolved.routeId()).isEqualTo("order-gateway");
        assertThat(resolved.tier()).isEqualTo(TenantTier.ENTERPRISE);
        assertThat(resolved.policy().version()).isEqualTo(5);
    }

    private Route route(String routeId) {
        return Route.async()
                .id(routeId)
                .uri(URI.create("http://localhost"))
                .predicate(exchange -> true)
                .build();
    }
}
