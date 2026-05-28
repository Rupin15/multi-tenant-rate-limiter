package com.springboot.multi_tenant_rate_limiter.configuration_server.policy.repository;

import com.springboot.multi_tenant_rate_limiter.configuration_server.policy.RateLimitPolicy;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @EmbeddedId
    private RateLimitPolicyId id;

    @Column(nullable = false)
    private String name;

    @Column(name = "max_tokens", nullable = false)
    private long maxTokens;

    @Column(name = "refill_tokens_per_second", nullable = false)
    private long refillTokensPerSecond;

    @Column(name = "lease_size", nullable = false)
    private long leaseSize;

    @Column(nullable = false)
    @Builder.Default
    private long version = 1;

    @Version
    @Column(name = "entity_version", nullable = false)
    @Builder.Default
    private Long entityVersion = 0L;

    @Column(name = "last_updated", nullable = false)
    private OffsetDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        lastUpdated = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public RateLimitPolicy toPolicy() {
        return new RateLimitPolicy(
                id.getRouteId(),
                id.getTier(),
                name,
                maxTokens,
                refillTokensPerSecond,
                leaseSize,
                version
        );
    }
}
