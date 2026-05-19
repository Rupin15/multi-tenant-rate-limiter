package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.scheduleHealthCheck;

import com.springboot.multi_tenant_rate_limiter.aspects.RateLimiterMetrics;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.luaScripting.RedisHealthState;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

@Component
@RequiredArgsConstructor
public class RedisHealthCheckScheduler {

    private final ReactiveRedisTemplate<String, Long> redisTemplate;
    private final RedisHealthState redisHealthState;
    private final RateLimiterMetrics metrics;

    @Scheduled(fixedDelayString = "${redis.health-check.delay:5000}")
    @Timed(
            value = "redis.health_check.time",
            description = "Time spent checking Redis health"
    )
    @Counted(
            value = "redis.health_check.runs",
            description = "Redis health check executions"
    )
    public void checkRedisHealth() {

        redisTemplate
                .execute(ReactiveRedisConnection::ping
                )
                .next()
                .doOnSuccess(response -> markHealthy())
                .doOnError(error -> markUnhealthy())
                .subscribe();
    }

    private void markHealthy() {
        redisHealthState.markHealthy();
        metrics.recordRedisHealthCheck("healthy");
    }

    private void markUnhealthy() {
        redisHealthState.markUnhealthy();
        metrics.recordRedisHealthCheck("unhealthy");
    }
}