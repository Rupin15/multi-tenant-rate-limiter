package com.springboot.multi_tenant_rate_limiter.configuration_server.policy.events;

import com.springboot.multi_tenant_rate_limiter.configuration_server.policy.RateLimitPolicy;
import com.springboot.multi_tenant_rate_limiter.configuration_server.policy.TenantTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitPolicyEvent {
    private String eventId;
    private String routeId;
    private TenantTier tier;
    private long version;
    private RateLimitPolicy policy;
    private OffsetDateTime createdAt;
}
