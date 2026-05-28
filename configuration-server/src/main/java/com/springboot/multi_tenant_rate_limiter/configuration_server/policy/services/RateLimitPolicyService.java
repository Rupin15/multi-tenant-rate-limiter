package com.springboot.multi_tenant_rate_limiter.configuration_server.policy.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.multi_tenant_rate_limiter.configuration_server.metrics.ConfigurationServerMetrics;
import com.springboot.multi_tenant_rate_limiter.configuration_server.policy.RateLimitPolicy;
import com.springboot.multi_tenant_rate_limiter.configuration_server.policy.dto.UpdateRateLimitPolicyRequest;
import com.springboot.multi_tenant_rate_limiter.configuration_server.policy.events.PolicyOutboxCreatedEvent;
import com.springboot.multi_tenant_rate_limiter.configuration_server.policy.events.RateLimitPolicyEvent;
import com.springboot.multi_tenant_rate_limiter.configuration_server.policy.outbox.PolicyEventOutboxEntity;
import com.springboot.multi_tenant_rate_limiter.configuration_server.policy.outbox.PolicyEventOutboxRepository;
import com.springboot.multi_tenant_rate_limiter.configuration_server.policy.repository.RateLimitPolicyEntity;
import com.springboot.multi_tenant_rate_limiter.configuration_server.policy.repository.RateLimitPolicyId;
import com.springboot.multi_tenant_rate_limiter.configuration_server.policy.repository.RateLimitPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RateLimitPolicyService {

    private final RateLimitPolicyRepository repository;
    private final PolicyEventOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ConfigurationServerMetrics metrics;

    @Transactional(readOnly = true)
    public List<RateLimitPolicy> fetchAllPolicies() {
        return repository.findAll().stream()
                .map(RateLimitPolicyEntity::toPolicy)
                .toList();
    }

    @Transactional
    public RateLimitPolicy upsertPolicy(UpdateRateLimitPolicyRequest request) {
        try {
            RateLimitPolicyId id = new RateLimitPolicyId(request.routeId(), request.tier());
            RateLimitPolicyEntity existing = repository.findById(id).orElse(null);
            long nextVersion = existing == null ? 1L : existing.getVersion() + 1;

            RateLimitPolicyEntity entity = existing == null
                    ? RateLimitPolicyEntity.builder()
                    .id(id)
                    .name(request.name())
                    .maxTokens(request.maxTokens())
                    .refillTokensPerSecond(request.refillTokensPerSecond())
                    .leaseSize(request.leaseSize())
                    .version(nextVersion)
                    .build()
                    : updateExisting(existing, request, nextVersion);

            repository.save(entity);
            RateLimitPolicy policy = entity.toPolicy();
            savePolicyEvent(policy);

            metrics.recordPolicyUpdate(policy.routeId(), policy.tier().name());
            return policy;
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new IllegalStateException("Policy update conflict detected. Please retry.", exception);
        }
    }

    private RateLimitPolicyEntity updateExisting(
            RateLimitPolicyEntity entity,
            UpdateRateLimitPolicyRequest request,
            long nextVersion
    ) {
        entity.setName(request.name());
        entity.setMaxTokens(request.maxTokens());
        entity.setRefillTokensPerSecond(request.refillTokensPerSecond());
        entity.setLeaseSize(request.leaseSize());
        entity.setVersion(nextVersion);
        return entity;
    }

    private void savePolicyEvent(RateLimitPolicy policy) {
        RateLimitPolicyEvent event = RateLimitPolicyEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .routeId(policy.routeId())
                .tier(policy.tier())
                .version(policy.version())
                .policy(policy)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        String payload = serializeEvent(event);
        PolicyEventOutboxEntity outboxEntity = PolicyEventOutboxEntity.builder()
                .id(event.getEventId())
                .routeId(policy.routeId())
                .tier(policy.tier())
                .payload(payload)
                .processed(false)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        outboxRepository.save(outboxEntity);
        eventPublisher.publishEvent(new PolicyOutboxCreatedEvent(outboxEntity.getId()));
    }

    private String serializeEvent(RateLimitPolicyEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize policy event", exception);
        }
    }
}
