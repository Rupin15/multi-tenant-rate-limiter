package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy;

import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

@Component
public class RateLimitPolicyResolver {

    public RateLimitPolicy resolve(ServerWebExchange exchange) {

        Route route =exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route == null) {
            return RateLimitPolicy.ORDERS;
        }

        return switch (route.getId()) {

            case "payment-gateway" ->
                    RateLimitPolicy.PAYMENTS;

            case "order-gateway" ->
                    RateLimitPolicy.ORDERS;

            default ->
                    RateLimitPolicy.ORDERS;
        };
    }
}