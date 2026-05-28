package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.services;

import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.config.ConfigServerProperties;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.repository.RateLimitPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ConfigServerPolicyClient {

    private static final ParameterizedTypeReference<List<RateLimitPolicy>> POLICY_LIST_TYPE = new ParameterizedTypeReference<>() {};

    private final WebClient.Builder webClientBuilder;
    private final ConfigServerProperties properties;

    public Mono<List<RateLimitPolicy>> fetchPolicies() {
        return webClientBuilder.build()
                .get()
                .uri(properties.getBaseUrl())
                .retrieve()
                .bodyToMono(POLICY_LIST_TYPE)
                .timeout(properties.getRequestTimeout());
    }
}
