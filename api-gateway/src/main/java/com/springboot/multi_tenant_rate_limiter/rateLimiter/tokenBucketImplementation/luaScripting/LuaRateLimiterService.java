package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.luaScripting;

import com.springboot.multi_tenant_rate_limiter.chaos.ChaosInjectionService;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.localRateLimiting.RateLimiterProperties;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.repository.RateLimitPolicy;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.bulkhead.operator.BulkheadOperator;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
public class LuaRateLimiterService {
    private static final String KEY_PREFIX = "rate-limit:";
    private static final String REDIS_LEASE_RESILIENCE_NAME = "redisLease";
    private final ReactiveRedisTemplate<String, Long> redisTemplate;
    private final RedisLuaScriptConfig scriptConfig;
    private final RateLimiterProperties properties;
    private final Bulkhead redisLeaseBulkhead;
    private final TimeLimiter redisLeaseTimeLimiter;
    private final Retry redisLeaseRetry;
    private final CircuitBreaker redisLeaseCircuitBreaker;
    private final ChaosInjectionService chaosInjectionService;

    public LuaRateLimiterService(
            ReactiveRedisTemplate<String, Long> redisTemplate,
            RedisLuaScriptConfig scriptConfig,
            RateLimiterProperties properties,
            BulkheadRegistry bulkheadRegistry,
            TimeLimiterRegistry timeLimiterRegistry,
            RetryRegistry retryRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry,
            ChaosInjectionService chaosInjectionService
    ) {
        this.redisTemplate = redisTemplate;
        this.scriptConfig = scriptConfig;
        this.properties = properties;
        this.chaosInjectionService = chaosInjectionService;
        this.redisLeaseBulkhead = bulkheadRegistry.bulkhead(REDIS_LEASE_RESILIENCE_NAME);
        this.redisLeaseTimeLimiter = timeLimiterRegistry.timeLimiter(REDIS_LEASE_RESILIENCE_NAME);
        this.redisLeaseRetry = retryRegistry.retry(REDIS_LEASE_RESILIENCE_NAME);
        this.redisLeaseCircuitBreaker = circuitBreakerRegistry.circuitBreaker(REDIS_LEASE_RESILIENCE_NAME);
    }

    @Timed(value = "rate_limiter.redis.lease.time", description = "Redis lease execution time")
    @Counted(value = "rate_limiter.redis.lease.calls", description = "Redis lease invocation count")

    public Mono<Long> requestLease(String ip, RateLimitPolicy policy) {
        if (chaosInjectionService.isRedisLeaseFailureEnabled()) {
            return Mono.error(new IllegalStateException("Chaos injection: redis lease failure enabled"));
        }
        String bucketKey = KEY_PREFIX + policy.name().toLowerCase() + ":" + ip;
        List<Long> arguments = List.of(policy.maxTokens(), policy.refillTokensPerSecond(), policy.leaseSize(), properties.getTtlSeconds());
        return redisTemplate.execute(scriptConfig.leaseScript(), List.of(bucketKey), arguments)
                .next()
                .defaultIfEmpty(0L)
                .transformDeferred(BulkheadOperator.of(redisLeaseBulkhead))
                .transformDeferred(CircuitBreakerOperator.of(redisLeaseCircuitBreaker))
                .transformDeferred(TimeLimiterOperator.of(redisLeaseTimeLimiter))
                .transformDeferred(RetryOperator.of(redisLeaseRetry))
                .doOnSuccess(result -> log.debug("Redis lease accepted key={} tokens={}", bucketKey, result))
                .doOnError(error -> log.warn("Redis lease failed key={} policy={} reason={}", bucketKey, policy.name(), error.getMessage()));
    }
}
