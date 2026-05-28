package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.luaScripting;

import com.springboot.multi_tenant_rate_limiter.aspects.RateLimiterMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisHealthMonitor {

    private final ReactiveRedisConnectionFactory connectionFactory;
    private final RedisHealthState redisHealthState;
    private final RateLimiterMetrics metrics;

    @Scheduled(fixedDelay = 5000)
    public void monitorRedis() {

        Mono.from(
                        connectionFactory.getReactiveConnection()

                                .ping()
                )
                .doOnNext(result -> {

                    redisHealthState.markHealthy();

                    metrics.recordRedisHealthCheck("success");

                    log.debug("redis ping successful");
                })
                .doOnError(error -> {

                    redisHealthState.markUnhealthy();

                    metrics.recordRedisHealthCheck("failure");

                    log.warn("redis ping failed reason={}",
                            error.getMessage());
                })
                .onErrorResume(error -> Mono.empty())
                .subscribe();
    }
}