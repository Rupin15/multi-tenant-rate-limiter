package com.springboot.multi_tenant_rate_limiter.filters;

import com.springboot.multi_tenant_rate_limiter.aspects.RateLimiterMetrics;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.localRateLimiting.IpBasedTokenBucket;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.localRateLimiting.TokenBucket;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.luaScripting.LuaRateLimiterService;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.luaScripting.RedisHealthState;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.RateLimitPolicy;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.RateLimitPolicyResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RateLimitFilter implements GlobalFilter, Ordered {


    private final IpBasedTokenBucket ipBuckets;
    private final LuaRateLimiterService luaRateLimiter;
    private final RedisHealthState redisHealthState;
    private final RateLimiterMetrics metrics;
    private final RateLimitPolicyResolver policyResolver;

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain
    ) {

        String clientIp =extractClientIp(exchange.getRequest());
        RateLimitPolicy policy =policyResolver.resolve(exchange);

        return applyRateLimit(clientIp, policy)
                .flatMap(decision -> {
                    metrics.recordDecision(decision.backend(),decision.outcome());
                    if (decision.blocked()) {
                        exchange.getResponse().setStatusCode( HttpStatus.TOO_MANY_REQUESTS);
                        return exchange.getResponse()
                                .setComplete();
                    }

                    return chain.filter(exchange);
                });
    }

    private Mono<RateLimitDecision> applyRateLimit( String clientIp, RateLimitPolicy policy) {
        TokenBucket localBucket = ipBuckets.getBucket(clientIp,policy);
        boolean localAllowed =localBucket.tryConsume();
        if (localAllowed) {
            return allow("local");
        }
        return requestRedisLease(clientIp,policy,localBucket);
    }

    private Mono<RateLimitDecision> requestRedisLease( String clientIp,RateLimitPolicy policy,TokenBucket localBucket) {
        if (!redisHealthState.isRedisHealthy()) {
            metrics.recordRedisFallback();
            return reject("redis_unavailable");
        }

        return luaRateLimiter
                .requestLease(clientIp, policy)
                .map(localBucket::addTokensAndConsume)
                .map(redisAllowed ->
                        redisAllowed? new RateLimitDecision(false,"redis","allowed")
                                : new RateLimitDecision(true,"redis","rejected"
                        )
                )
                .onErrorResume(error -> {
                    redisHealthState.markUnhealthy();
                    metrics.recordRedisFallback();
                    return reject("redis_error");
                });
    }

    private Mono<RateLimitDecision> allow(String backend) {
        return Mono.just(new RateLimitDecision(false,backend,"allowed"));
    }

    private Mono<RateLimitDecision> reject( String backend) {
        return Mono.just(new RateLimitDecision(true,backend,"rejected")
        );
    }

    private String extractClientIp( ServerHttpRequest request) {
        String forwardedFor =request.getHeaders() .getFirst("X-Forwarded-For");
        if (forwardedFor != null &&!forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp =request.getHeaders().getFirst("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null &&remoteAddress.getAddress() != null) {
            return remoteAddress .getAddress().getHostAddress();
        }
        return "unknown";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private record RateLimitDecision( boolean blocked,String backend,String outcome) { }
}