package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.subscriber;

import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.luaScripting.RedisHealthState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisPubSubListenerManager {

    private final RedisMessageListenerContainer listenerContainer;
    private final RedisHealthState redisHealthState;
    private final RedisPubSubReconnectProperties reconnectProperties;
    private final Clock clock = Clock.systemUTC();

    private final AtomicLong nextAllowedAttemptAtMs = new AtomicLong(0);
    private final AtomicLong currentDelayMs = new AtomicLong(0);

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        // Try once on startup; further attempts are gated by health checks + backoff.
        startIfHealthy();
    }

    public void startIfHealthy() {
        if (!redisHealthState.isRedisHealthy()) {
            return;
        }
        startWithBackoffIfDue();
    }

    private synchronized void startWithBackoffIfDue() {
        if (listenerContainer.isRunning()) {
            resetBackoff();
            return;
        }

        long now = clock.millis();
        long allowedAt = nextAllowedAttemptAtMs.get();
        if (now < allowedAt) {
            return;
        }

        try {
            listenerContainer.start();
            resetBackoff();
            log.info("Redis policy pub/sub listener started");
        } catch (Exception ex) {
            long delay = computeNextDelayMs();
            nextAllowedAttemptAtMs.set(now + delay);
            log.warn("Redis policy pub/sub listener start failed. nextAttemptInMs={} reason={}", delay, ex.getMessage());
        }
    }

    private void resetBackoff() {
        currentDelayMs.set(0);
        nextAllowedAttemptAtMs.set(0);
    }

    private long computeNextDelayMs() {
        long current = currentDelayMs.get();
        long baseDelayMs = reconnectProperties.getBaseDelayMs();
        long maxDelayMs = reconnectProperties.getMaxDelayMs();
        double multiplier = reconnectProperties.getMultiplier();

        long next = (current <= 0) ? baseDelayMs : (long) Math.ceil(current * multiplier);
        if (next < baseDelayMs) {
            next = baseDelayMs;
        }
        if (next > maxDelayMs) {
            next = maxDelayMs;
        }
        currentDelayMs.set(next);
        return next;
    }
}
