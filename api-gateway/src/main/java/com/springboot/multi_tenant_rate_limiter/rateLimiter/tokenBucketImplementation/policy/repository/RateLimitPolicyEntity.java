package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.repository;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "rate_limit_policies")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitPolicyEntity {

    @Id
    @Column(name = "route_id", nullable = false)
    private String routeId;

    @Column(nullable = false)
    private String name;

    @Column(name = "max_tokens", nullable = false)
    private long maxTokens;

    @Column(
            name = "refill_tokens_per_second",
            nullable = false
    )
    private long refillTokensPerSecond;

    @Column(name = "lease_size", nullable = false)
    private long leaseSize;

    @Column(nullable = false)
    @Builder.Default
    private long version = 1;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long entityVersion = 0L;

    @Column(name = "last_updated", nullable = false)
    private OffsetDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {

        lastUpdated =
                OffsetDateTime.now(ZoneOffset.UTC);
    }

    public static RateLimitPolicyEntity from(
            String routeId,
            RateLimitPolicy policy
    ) {

        return RateLimitPolicyEntity.builder()
                .routeId(routeId)
                .name(policy.name())
                .maxTokens(policy.maxTokens())
                .refillTokensPerSecond(
                        policy.refillTokensPerSecond()
                )
                .leaseSize(policy.leaseSize())
                .version(policy.version())
                .build();
    }

    public RateLimitPolicy toPolicy() {

        return new RateLimitPolicy(
                name,
                maxTokens,
                refillTokensPerSecond,
                leaseSize,
                version
        );
    }
}