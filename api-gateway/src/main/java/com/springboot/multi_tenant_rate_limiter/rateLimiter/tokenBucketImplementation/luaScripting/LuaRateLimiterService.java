package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.luaScripting;

import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.localRateLimiting.RateLimiterProperties;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.RateLimitPolicy;
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

    public Mono<Long> requestLease(
            String ip,
            RateLimitPolicy policy
    ) {

        String bucketKey =
                KEY_PREFIX
                        + policy.name().toLowerCase()
                        + ":"
                        + ip;

        List<Long> arguments = List.of(
                policy.maxTokens(),
                policy.refillTokensPerSecond(),
                policy.leaseSize(),
                properties.getTtlSeconds()
        );

        return redisTemplate.execute(
                        scriptConfig.leaseScript(),
                        List.of(bucketKey),
                        arguments
                )
                .next()
                .defaultIfEmpty(0L);
    }
}