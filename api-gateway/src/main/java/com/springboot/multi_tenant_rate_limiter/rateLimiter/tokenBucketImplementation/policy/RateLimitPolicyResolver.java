package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

@Component
public class RateLimitPolicyResolver {
    @Autowired
    private RateLimitPolicyRegistry rateLimitPolicyRegistry;
    public RateLimitPolicy resolve(ServerWebExchange exchange) {
        Route route =exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        return rateLimitPolicyRegistry.getPolicy(route);
    }

    public void updatePolicy(String policyName, RateLimitPolicy rateLimitPolicy) {
        rateLimitPolicyRegistry.updatePolicy(policyName, rateLimitPolicy);
    }
}