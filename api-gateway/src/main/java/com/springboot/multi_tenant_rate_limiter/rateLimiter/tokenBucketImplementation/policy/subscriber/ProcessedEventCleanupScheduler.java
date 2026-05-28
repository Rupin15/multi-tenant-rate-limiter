package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.subscriber;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProcessedEventCleanupScheduler {

    private final RedisPolicySubscriber subscriber;

    @Scheduled(fixedDelay = 3600000)
    public void cleanup() {

        Instant cutoff = Instant.now().minusSeconds(3600);
        subscriber.getProcessedEvents()
                .entrySet()
                .removeIf(entry -> entry.getValue().isBefore(cutoff));
    }
}