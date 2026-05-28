package com.springboot.multi_tenant_rate_limiter.configuration_server.policy.repository;

import com.springboot.multi_tenant_rate_limiter.configuration_server.policy.TenantTier;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitPolicyId implements Serializable {

    @Column(name = "route_id", nullable = false)
    private String routeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, length = 32)
    private TenantTier tier;
}
