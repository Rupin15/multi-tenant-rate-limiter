package com.springboot.multi_tenant_rate_limiter.configuration_server.policy.publisher;

import com.springboot.multi_tenant_rate_limiter.configuration_server.metrics.ConfigurationServerMetrics;
import com.springboot.multi_tenant_rate_limiter.configuration_server.policy.events.PolicyOutboxCreatedEvent;
import com.springboot.multi_tenant_rate_limiter.configuration_server.policy.outbox.PolicyEventOutboxEntity;
import com.springboot.multi_tenant_rate_limiter.configuration_server.policy.outbox.PolicyEventOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
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
    private final ConfigurationServerMetrics metrics;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOutboxCreated(PolicyOutboxCreatedEvent event) {
        try {
            PolicyEventOutboxEntity outbox = outboxRepository.findById(event.outboxId()).orElse(null);
            if (outbox == null || outbox.isProcessed()) {
                return;
            }

            redisTemplate.convertAndSend(CHANNEL, outbox.getPayload());
            outbox.setProcessed(true);
            outboxRepository.save(outbox);
            metrics.recordOutboxPublish("success");
            log.info("published policy event id={} routeId={} tier={}", outbox.getId(), outbox.getRouteId(), outbox.getTier());
        } catch (DataAccessException exception) {
            metrics.recordOutboxPublish("redis_unavailable");
            log.warn("redis unavailable while publishing policy event id={}", event.outboxId());
        } catch (Exception exception) {
            metrics.recordOutboxPublish("error");
            log.error("unexpected policy publish failure id={}", event.outboxId(), exception);
        }
    }
}
