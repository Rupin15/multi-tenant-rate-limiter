package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy;

import jakarta.annotation.PostConstruct;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitPolicyRegistry {
    private final Map<String, RateLimitPolicy> policyRegistry;
    private final RateLimitPolicy defaultPolicy;
    public RateLimitPolicyRegistry(){
        policyRegistry= new ConcurrentHashMap<>();
        defaultPolicy = new RateLimitPolicy("DEFAULT",20,2,1);
    }

    @PostConstruct
    public void fillRegistry(){
        policyRegistry.put("payment-gateway", new RateLimitPolicy("PAYMENTS",50,2,3));
        policyRegistry.put("order-gateway", new RateLimitPolicy("ORDERS", 100,5,10));
//        policyRegistry.put("default-policy", new RateLimitPolicy("DEFAULT", 100,5,10));
    }

    public RateLimitPolicy getPolicy(Route route){
        if (route == null) return defaultPolicy;
        return policyRegistry.getOrDefault(route.getId(),defaultPolicy);
    }

    public void updatePolicy(String policyName, RateLimitPolicy rateLimitPolicy) {
        policyRegistry.put(policyName,rateLimitPolicy);
    }
}
