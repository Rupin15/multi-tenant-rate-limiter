package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.events;

public record PolicyOutboxCreatedEvent(
        String outboxId
) {}