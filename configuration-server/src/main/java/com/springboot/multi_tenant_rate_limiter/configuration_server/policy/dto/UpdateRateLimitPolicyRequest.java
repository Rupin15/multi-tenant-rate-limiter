package com.springboot.multi_tenant_rate_limiter.configuration_server.policy.dto;

import com.springboot.multi_tenant_rate_limiter.configuration_server.policy.TenantTier;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateRateLimitPolicyRequest(
        @NotBlank String routeId,
        @NotNull TenantTier tier,
        @NotBlank String name,
        @Min(1) long maxTokens,
        @Min(1) long refillTokensPerSecond,
        @Min(1) long leaseSize
) {
}
