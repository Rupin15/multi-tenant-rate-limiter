package com.springboot.multi_tenant_rate_limiter.configuration_server.policy.publisher;

import com.springboot.multi_tenant_rate_limiter.configuration_server.metrics.ConfigurationServerMetrics;
import com.springboot.multi_tenant_rate_limiter.configuration_server.policy.outbox.PolicyEventOutboxEntity;
import com.springboot.multi_tenant_rate_limiter.configuration_server.policy.outbox.PolicyEventOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
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
    private final ConfigurationServerMetrics metrics;

    @Scheduled(fixedDelayString = "${rate-limiter.config.outbox.retry-delay-ms:10000}")
    @Transactional
    public void retryFailedEvents() {
        List<PolicyEventOutboxEntity> events = outboxRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc();
        if (events.isEmpty()) {
            return;
        }

        for (PolicyEventOutboxEntity event : events) {
            try {
                redisTemplate.convertAndSend(CHANNEL, event.getPayload());
                event.setProcessed(true);
                outboxRepository.save(event);
                metrics.recordOutboxPublish("retry_success");
                log.info("republished policy event id={} routeId={} tier={}", event.getId(), event.getRouteId(), event.getTier());
            } catch (DataAccessException exception) {
                metrics.recordOutboxPublish("retry_redis_unavailable");
                log.warn("redis unavailable while retrying policy event id={}", event.getId());
                return;
            } catch (Exception exception) {
                metrics.recordOutboxPublish("retry_error");
                log.error("unexpected policy retry failure id={}", event.getId(), exception);
            }
        }
    }
}
