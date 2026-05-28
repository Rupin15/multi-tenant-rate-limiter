package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.subscriber;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rate-limiter.redis.pubsub.reconnect")
public class RedisPubSubReconnectProperties {

    private long baseDelayMs = 1000;
    private long maxDelayMs = 30000;
    private double multiplier = 2.0;

    public long getBaseDelayMs() {
        return baseDelayMs;
    }

    public void setBaseDelayMs(long baseDelayMs) {
        this.baseDelayMs = baseDelayMs;
    }

    public long getMaxDelayMs() {
        return maxDelayMs;
    }

    public void setMaxDelayMs(long maxDelayMs) {
        this.maxDelayMs = maxDelayMs;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }
}
