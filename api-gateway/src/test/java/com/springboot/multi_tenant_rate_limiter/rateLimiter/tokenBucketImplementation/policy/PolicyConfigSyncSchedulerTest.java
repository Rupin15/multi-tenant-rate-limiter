package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy;

import com.springboot.multi_tenant_rate_limiter.aspects.RateLimiterMetrics;
import com.springboot.multi_tenant_rate_limiter.chaos.ChaosInjectionService;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.repository.RateLimitPolicy;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.repository.TenantTier;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.services.ConfigServerPolicyClient;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.services.RateLimitPolicyRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PolicyConfigSyncSchedulerTest {

    @Test
    void shouldSyncPoliciesFromConfigurationServer() {
        ConfigServerPolicyClient client = mock(ConfigServerPolicyClient.class);
        RateLimitPolicyRegistry registry = new RateLimitPolicyRegistry();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RateLimiterMetrics metrics = new RateLimiterMetrics(meterRegistry);
        ChaosInjectionService chaosInjectionService = new ChaosInjectionService();

        RateLimitPolicy syncedPolicy = new RateLimitPolicy("order-gateway", TenantTier.PRO, "ORDERS_PRO", 120, 8, 8, 3);
        when(client.fetchPolicies()).thenReturn(reactor.core.publisher.Mono.just(List.of(syncedPolicy)));

        PolicyConfigSyncScheduler scheduler = new PolicyConfigSyncScheduler(client, registry, metrics, chaosInjectionService);
        scheduler.syncPoliciesFromConfigurationServer();

        RateLimitPolicy policy = registry.getPolicy("order-gateway", TenantTier.PRO);
        assertThat(policy.version()).isEqualTo(3);
        assertThat(policy.maxTokens()).isEqualTo(120);
        assertThat(meterRegistry.find("rate_limiter.config.sync").tags("outcome", "success").counter().count()).isEqualTo(1.0d);
    }

    @Test
    void shouldSkipSyncWhenChaosFailureIsEnabled() {
        ConfigServerPolicyClient client = mock(ConfigServerPolicyClient.class);
        RateLimitPolicyRegistry registry = new RateLimitPolicyRegistry();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RateLimiterMetrics metrics = new RateLimiterMetrics(meterRegistry);
        ChaosInjectionService chaosInjectionService = new ChaosInjectionService();
        chaosInjectionService.updateState(false, true);

        PolicyConfigSyncScheduler scheduler = new PolicyConfigSyncScheduler(client, registry, metrics, chaosInjectionService);
        scheduler.syncPoliciesFromConfigurationServer();

        verifyNoInteractions(client);
        assertThat(meterRegistry.find("rate_limiter.config.sync").tags("outcome", "chaos_failure").counter().count()).isEqualTo(1.0d);
    }
}
