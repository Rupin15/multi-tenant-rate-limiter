package com.springboot.multi_tenant_rate_limiter.aspects;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class RateLimiterMetrics {
    private final MeterRegistry meterRegistry;

    public RateLimiterMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordDecision(String backend, String outcome) {
        meterRegistry.counter("rate_limiter.decisions", "backend", backend, "outcome", outcome).increment();
    }

    public void recordRedisFallback() {
        meterRegistry.counter("rate_limiter.redis.fallbacks").increment();
    }

    public void recordRedisHealthCheck(String outcome) {
        meterRegistry.counter("redis.health_checks", "outcome", outcome).increment();
    }
}
