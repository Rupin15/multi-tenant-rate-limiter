package com.springboot.multi_tenant_rate_limiter.configuration_server.policy.events;

public record PolicyOutboxCreatedEvent(String outboxId) {
}
