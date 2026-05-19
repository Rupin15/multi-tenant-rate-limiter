package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.luaScripting;

import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.localRateLimiting.RateLimiterProperties;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LuaRateLimiterService {

    private static final String KEY_PREFIX = "rate-limit:";

    private final ReactiveRedisTemplate<String, Long> redisTemplate;
    private final RedisLuaScriptConfig scriptConfig;
    private final RateLimiterProperties properties;

    @Timed(
            value = "redis.lease.execution_time",
            description = "Time spent leasing tokens from Redis"
    )
    @Counted(
            value = "redis.lease.requests",
            description = "Redis token lease requests"
    )
    public Mono<Long> requestLease(String userId) {
        String bucketKey = buildBucketKey(userId);
        List<Long> scriptArguments = List.of(
                properties.getMaxTokens(),
                properties.getRefillTokensPerMilliSecond(),
                properties.getLeaseSize(),
                properties.getTtlSeconds()
        );

        return redisTemplate
                .execute(
                        scriptConfig.leaseScript(),
                        List.of(bucketKey),
                        scriptArguments
                )
                .next()
                .defaultIfEmpty(0L);
    }

    private String buildBucketKey(String userId) {
        return KEY_PREFIX + userId;
    }
}