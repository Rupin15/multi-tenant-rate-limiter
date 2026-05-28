package com.springboot.multi_tenant_rate_limiter.configuration_server.policy.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RateLimitPolicyRepository extends JpaRepository<RateLimitPolicyEntity, RateLimitPolicyId> {
}
