package com.springboot.multi_tenant_rate_limiter.configuration_server.policy.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.multi_tenant_rate_limiter.configuration_server.metrics.ConfigurationServerMetrics;
import com.springboot.multi_tenant_rate_limiter.configuration_server.policy.RateLimitPolicy;
import com.springboot.multi_tenant_rate_limiter.configuration_server.policy.TenantTier;
import com.springboot.multi_tenant_rate_limiter.configuration_server.policy.dto.UpdateRateLimitPolicyRequest;
import com.springboot.multi_tenant_rate_limiter.configuration_server.policy.outbox.PolicyEventOutboxRepository;
import com.springboot.multi_tenant_rate_limiter.configuration_server.policy.repository.RateLimitPolicyEntity;
import com.springboot.multi_tenant_rate_limiter.configuration_server.policy.repository.RateLimitPolicyId;
import com.springboot.multi_tenant_rate_limiter.configuration_server.policy.repository.RateLimitPolicyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitPolicyServiceTest {

    @Test
    void shouldUpsertPolicyAndCreateOutboxEvent() throws Exception {
        RateLimitPolicyRepository repository = mock(RateLimitPolicyRepository.class);
        PolicyEventOutboxRepository outboxRepository = mock(PolicyEventOutboxRepository.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        ConfigurationServerMetrics metrics = mock(ConfigurationServerMetrics.class);

        when(repository.findById(any(RateLimitPolicyId.class))).thenReturn(Optional.empty());
        when(repository.save(any(RateLimitPolicyEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(outboxRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"event\":\"ok\"}");

        RateLimitPolicyService service = new RateLimitPolicyService(
                repository,
                outboxRepository,
                objectMapper,
                eventPublisher,
                metrics
        );

        UpdateRateLimitPolicyRequest request = new UpdateRateLimitPolicyRequest(
                "payment-gateway",
                TenantTier.PRO,
                "PAYMENTS_PRO",
                100,
                10,
                8
        );

        RateLimitPolicy policy = service.upsertPolicy(request);

        assertThat(policy.routeId()).isEqualTo("payment-gateway");
        assertThat(policy.tier()).isEqualTo(TenantTier.PRO);
        assertThat(policy.version()).isEqualTo(1);
        verify(repository).save(any(RateLimitPolicyEntity.class));
        verify(outboxRepository).save(any());
        verify(eventPublisher).publishEvent(any(Object.class));
        verify(metrics).recordPolicyUpdate("payment-gateway", "PRO");
    }
}
