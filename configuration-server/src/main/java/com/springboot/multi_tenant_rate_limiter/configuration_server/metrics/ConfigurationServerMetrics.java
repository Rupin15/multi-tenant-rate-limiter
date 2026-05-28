package com.springboot.multi_tenant_rate_limiter.configuration_server.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationServerMetrics {

    private final MeterRegistry meterRegistry;

    public ConfigurationServerMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordPolicyUpdate(String routeId, String tier) {
        meterRegistry.counter(
                "rate_limiter.config.policy_updates",
                "routeId",
                routeId,
                "tier",
                tier
        ).increment();
    }

    public void recordOutboxPublish(String outcome) {
        meterRegistry.counter(
                "rate_limiter.config.outbox_publish",
                "outcome",
                outcome
        ).increment();
    }
}
