package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.luaScripting;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RedisHealthState {
    private volatile boolean redisHealthy = true;

    @Autowired
    public RedisHealthState(MeterRegistry registry) {
        Gauge.builder("redis.status", () -> redisHealthy ? 1.0 : 0.0)
                .description("Current status of redis")
                .register(registry);
    }

    public boolean isRedisHealthy() {
        return redisHealthy;
    }

    public void markHealthy() {
        redisHealthy = true;
    }

    public void markUnhealthy() {
        redisHealthy = false;
    }
}