package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.publisher;

import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.luaScripting.RedisHealthState;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.events.PolicyOutboxCreatedEvent;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.outbox.PolicyEventOutboxEntity;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.outbox.PolicyEventOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImmediatePolicyPublisher {

    private static final String CHANNEL = "rate-limit-policy-updates";
    private final PolicyEventOutboxRepository outboxRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisHealthState redisHealthState;

    @Async
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleOutboxCreated(PolicyOutboxCreatedEvent event) {
        try {
            PolicyEventOutboxEntity outbox = outboxRepository.findById(event.outboxId()).orElse(null);
            if (outbox == null || outbox.isProcessed()) {
                return;
            }
            redisTemplate.convertAndSend(CHANNEL, outbox.getPayload());
            redisHealthState.markHealthy();
            outbox.setProcessed(true);
            outboxRepository.save(outbox);
            log.info("Immediately published event={}", outbox.getId());
        } catch (DataAccessException ex) {
            redisHealthState.markUnhealthy();
            log.warn("Redis unavailable. Event queued for retry. eventId={}", event.outboxId());
        } catch (Exception ex) {
            log.error("Unexpected publish failure", ex);
        }
    }
}