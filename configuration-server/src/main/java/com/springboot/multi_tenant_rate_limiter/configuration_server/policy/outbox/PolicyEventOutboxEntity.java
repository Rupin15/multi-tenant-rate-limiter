package com.springboot.multi_tenant_rate_limiter.configuration_server.policy.outbox;

import com.springboot.multi_tenant_rate_limiter.configuration_server.policy.TenantTier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @Column(name = "route_id", nullable = false)
    private String routeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, length = 32)
    private TenantTier tier;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(nullable = false)
    private boolean processed;

    @Column(nullable = false)
    private OffsetDateTime createdAt;
}
