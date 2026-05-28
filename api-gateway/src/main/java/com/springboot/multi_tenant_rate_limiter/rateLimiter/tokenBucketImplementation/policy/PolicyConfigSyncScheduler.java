package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy;

import com.springboot.multi_tenant_rate_limiter.aspects.RateLimiterMetrics;
import com.springboot.multi_tenant_rate_limiter.chaos.ChaosInjectionService;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.repository.RateLimitPolicy;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.services.ConfigServerPolicyClient;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.services.RateLimitPolicyRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyConfigSyncScheduler {

    private final ConfigServerPolicyClient policyClient;
    private final RateLimitPolicyRegistry registry;
    private final RateLimiterMetrics metrics;
    private final ChaosInjectionService chaosInjectionService;

    @PostConstruct
    public void initialSync() {
        syncPoliciesFromConfigurationServer();
    }

    @Scheduled(fixedDelayString = "${rate-limiter.config-server.sync-delay-ms:30000}")
    public void syncPoliciesFromConfigurationServer() {
        if (chaosInjectionService.isConfigSyncFailureEnabled()) {
            metrics.recordConfigSync("chaos_failure");
            log.warn("configuration sync skipped due to chaos injection");
            return;
        }

        try {
            List<RateLimitPolicy> policies = policyClient.fetchPolicies().block();
            if (policies == null) {
                metrics.recordConfigSync("empty_response");
                return;
            }

            registry.replacePolicies(policies);
            metrics.recordConfigSync("success");
            log.info("policy sync complete from configuration-server count={}", policies.size());
        } catch (Exception exception) {
            metrics.recordConfigSync("failure");
            log.warn("failed to sync policies from configuration-server reason={}", exception.getMessage());
        }
    }
}
