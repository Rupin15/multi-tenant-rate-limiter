package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.luaScripting;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class RedisHealthState {

    private final AtomicBoolean redisHealthy = new AtomicBoolean(true);

    public RedisHealthState(MeterRegistry registry) {

        Gauge.builder("redis.status",
                        () -> redisHealthy.get() ? 1.0 : 0.0)
                .description("Current status of redis")
                .register(registry);
    }

    public boolean isRedisHealthy() {
        return redisHealthy.get();
    }

    public void markHealthy() {
        redisHealthy.set(true);
    }

    public void markUnhealthy() {
        redisHealthy.set(false);
    }
}