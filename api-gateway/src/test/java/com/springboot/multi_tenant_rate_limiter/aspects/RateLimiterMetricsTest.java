package com.springboot.multi_tenant_rate_limiter.aspects;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterMetricsTest {

    @Test
    void shouldRecordRateLimiterAndConfigMetrics() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RateLimiterMetrics metrics = new RateLimiterMetrics(meterRegistry);

        metrics.recordDecision("payment-gateway", "FREE", "redis", "allowed");
        metrics.recordRedisFallback();
        metrics.recordRedisHealthCheck("healthy");
        metrics.recordConfigSync("success");

        double decisions = meterRegistry.find("rate_limiter.decisions")
                .tags("routeId", "payment-gateway", "tier", "FREE", "backend", "redis", "outcome", "allowed")
                .counter()
                .count();
        double fallbacks = meterRegistry.find("rate_limiter.redis.fallbacks").counter().count();
        double healthChecks = meterRegistry.find("redis.health_checks").tags("outcome", "healthy").counter().count();
        double configSync = meterRegistry.find("rate_limiter.config.sync").tags("outcome", "success").counter().count();

        assertThat(decisions).isEqualTo(1.0d);
        assertThat(fallbacks).isEqualTo(1.0d);
        assertThat(healthChecks).isEqualTo(1.0d);
        assertThat(configSync).isEqualTo(1.0d);
    }
}
