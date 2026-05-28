package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "policy_event_outbox")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyEventOutboxEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String policyName;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(nullable = false)
    private boolean processed;

    @Column(nullable = false)
    private OffsetDateTime createdAt;
}