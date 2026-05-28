package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.publisher;

import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.luaScripting.RedisHealthState;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.outbox.PolicyEventOutboxEntity;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.outbox.PolicyEventOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyOutboxPublisher {
    private static final String CHANNEL = "rate-limit-policy-updates";
    private final PolicyEventOutboxRepository outboxRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisHealthState redisHealthState;

    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void retryFailedEvents() {

        List<PolicyEventOutboxEntity> events = outboxRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc();
        if (events.isEmpty()) {
            return;
        }
        for (PolicyEventOutboxEntity event : events) {
            try {

                redisTemplate.convertAndSend(CHANNEL, event.getPayload());
                redisHealthState.markHealthy();
                event.setProcessed(true);
                outboxRepository.save(event);
                log.info("Republished event={}", event.getId());
            } catch (DataAccessException ex) {
                redisHealthState.markUnhealthy();
                log.warn("Redis still unavailable. eventId={}", event.getId());
                return;
            } catch (Exception ex) {
                log.error("Unexpected retry failure event={}", event.getId(), ex);
            }
        }
    }
}