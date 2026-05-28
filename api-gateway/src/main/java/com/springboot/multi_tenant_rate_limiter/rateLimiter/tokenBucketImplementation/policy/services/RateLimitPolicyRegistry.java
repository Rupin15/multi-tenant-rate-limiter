package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.services;

import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.repository.RateLimitPolicy;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.repository.RateLimitPolicyRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class RateLimitPolicyRegistry {

    private final RateLimitPolicyRepository repository;

    private final Map<String, RateLimitPolicy> policyRegistry =
            new ConcurrentHashMap<>();

    private static final RateLimitPolicy DEFAULT_POLICY =
            new RateLimitPolicy(
                    "DEFAULT",
                    20,
                    2,
                    1,
                    1
            );

    @PostConstruct
    public void loadPolicies() {

        repository.findAll()
                .forEach(entity ->
                        policyRegistry.put(
                                entity.getRouteId(),
                                entity.toPolicy()
                        )
                );
    }

    public RateLimitPolicy getPolicy(Route route) {

        if (route == null) {
            return DEFAULT_POLICY;
        }

        return policyRegistry.getOrDefault(
                route.getId(),
                DEFAULT_POLICY
        );
    }

    public void refreshPolicy(
            String routeId,
            RateLimitPolicy incomingPolicy
    ) {

        RateLimitPolicy existing =
                policyRegistry.get(routeId);

        if (existing == null ||
                incomingPolicy.version() > existing.version()) {

            policyRegistry.put(
                    routeId,
                    incomingPolicy
            );
        }
    }
}