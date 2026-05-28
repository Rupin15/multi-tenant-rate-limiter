package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.OffsetDateTime;
import java.util.List;

public interface RateLimitPolicyRepository extends JpaRepository<RateLimitPolicyEntity, String> {
    List<RateLimitPolicyEntity> findAllByLastUpdatedGreaterThanEqual(OffsetDateTime timestamp);
}