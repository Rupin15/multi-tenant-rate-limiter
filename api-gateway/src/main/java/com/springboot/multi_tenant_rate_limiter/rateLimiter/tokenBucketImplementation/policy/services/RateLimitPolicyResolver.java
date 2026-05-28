package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.events.PolicyOutboxCreatedEvent;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.events.RateLimitPolicyEvent;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.outbox.PolicyEventOutboxEntity;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.outbox.PolicyEventOutboxRepository;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.repository.RateLimitPolicy;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.repository.RateLimitPolicyEntity;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.repository.RateLimitPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ServerWebExchange;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RateLimitPolicyResolver {

    private final RateLimitPolicyRepository repository;

    private final PolicyEventOutboxRepository outboxRepository;

    private final RateLimitPolicyRegistry registry;

    private final ObjectMapper objectMapper;

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public String updatePolicy(String routeId, RateLimitPolicy policy) {
        try {
            RateLimitPolicyEntity existing = repository.findById(routeId).orElse(null);
            long nextVersion = existing == null
                            ? 1
                            : existing.getVersion() + 1;

            RateLimitPolicy updatedPolicy = new RateLimitPolicy(policy.name(), policy.maxTokens(), policy.refillTokensPerSecond(), policy.leaseSize(), nextVersion);
            RateLimitPolicyEntity entity;
            if (existing == null) {
                entity = RateLimitPolicyEntity.builder()
                        .routeId(routeId)
                        .name(updatedPolicy.name())
                        .maxTokens(updatedPolicy.maxTokens())
                        .refillTokensPerSecond(
                                updatedPolicy.refillTokensPerSecond()
                        )
                        .leaseSize(updatedPolicy.leaseSize())
                        .version(nextVersion)
                        .build();

            } else {
                entity = existing;
                entity.setName(updatedPolicy.name());
                entity.setMaxTokens(updatedPolicy.maxTokens());
                entity.setRefillTokensPerSecond(updatedPolicy.refillTokensPerSecond());
                entity.setLeaseSize(updatedPolicy.leaseSize());
                entity.setVersion(nextVersion);
            }

            repository.save(entity);
            RateLimitPolicyEvent event = RateLimitPolicyEvent.builder()
                            .eventId(UUID.randomUUID().toString())
                            .policyName(routeId)
                            .version(nextVersion)
                            .policy(updatedPolicy)
                            .createdAt(
                                    OffsetDateTime.now(
                                            ZoneOffset.UTC
                                    )
                            )
                            .build();

            String payload = objectMapper.writeValueAsString(event);

            PolicyEventOutboxEntity outbox =
                    PolicyEventOutboxEntity.builder()
                            .id(event.getEventId())
                            .policyName(routeId)
                            .payload(payload)
                            .processed(false)
                            .createdAt(
                                    OffsetDateTime.now(
                                            ZoneOffset.UTC
                                    )
                            )
                            .build();

            outboxRepository.save(outbox);

            registry.refreshPolicy(routeId, updatedPolicy);
            eventPublisher.publishEvent(new PolicyOutboxCreatedEvent(outbox.getId()));

            return """
                    Policy update committed successfully.
                    Propagation is handled asynchronously.
                    If Redis is unavailable, synchronization will retry automatically.
                    """;

        } catch (
                ObjectOptimisticLockingFailureException ex
        ) {

            return """
                    Policy update conflict detected.
                    Please retry.
                    """;

        } catch (Exception ex) {

            throw new RuntimeException(
                    "Failed to update policy",
                    ex
            );
        }
    }

    public RateLimitPolicy resolve(
            ServerWebExchange exchange
    ) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        return registry.getPolicy(route);
    }
}