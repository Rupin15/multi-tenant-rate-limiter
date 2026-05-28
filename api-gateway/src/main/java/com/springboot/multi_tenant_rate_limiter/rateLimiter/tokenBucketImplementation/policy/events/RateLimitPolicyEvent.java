package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.events;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.repository.RateLimitPolicy;
import lombok.*;
import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitPolicyEvent {
    private String eventId;
    private String policyName;
    private long version;
    private RateLimitPolicy policy;
    private OffsetDateTime createdAt;
}