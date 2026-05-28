package com.springboot.multi_tenant_rate_limiter.aspects;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class RateLimiterMetrics {
    private final MeterRegistry meterRegistry;

    public RateLimiterMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordDecision(String routeId, String tier, String backend, String outcome) {
        meterRegistry.counter(
                "rate_limiter.decisions",
                "routeId",
                routeId,
                "tier",
                tier,
                "backend",
                backend,
                "outcome",
                outcome
        ).increment();
    }

    public void recordRedisFallback() {
        meterRegistry.counter("rate_limiter.redis.fallbacks").increment();
    }

    public void recordRedisPubSubError() {
        meterRegistry.counter("rate_limiter.redis.pubsub.errors").increment();
    }

    public void recordRedisHealthCheck(String outcome) {
        meterRegistry.counter("redis.health_checks", "outcome", outcome).increment();
    }

    public void recordConfigSync(String outcome) {
        meterRegistry.counter("rate_limiter.config.sync", "outcome", outcome).increment();
    }
}
