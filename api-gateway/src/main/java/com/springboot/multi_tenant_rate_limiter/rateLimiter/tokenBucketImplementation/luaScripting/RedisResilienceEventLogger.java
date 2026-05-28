package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.luaScripting;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisResilienceEventLogger {

    private static final String REDIS_LEASE_RESILIENCE_NAME = "redisLease";
    private final BulkheadRegistry bulkheadRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;
    private final RetryRegistry retryRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @PostConstruct
    public void registerEventLoggers() {
        registerBulkheadEvents();
        registerTimeLimiterEvents();
        registerRetryEvents();
        registerCircuitBreakerEvents();
        registerCircuitBreakerEventsPaymentGateway();
        registerCircuitBreakerEventsOrderGateway();
    }

    private void registerBulkheadEvents() {
        Bulkhead bulkhead = bulkheadRegistry.bulkhead(REDIS_LEASE_RESILIENCE_NAME);
        bulkhead.getEventPublisher().onEvent(event -> {
            String eventType = event.getEventType().name();
            if ("CALL_REJECTED".equals(eventType)) {
                log.warn("redis bulkhead event={} name={}", eventType, event.getBulkheadName());
                return;
            }
            log.debug("redis bulkhead event={} name={}", eventType, event.getBulkheadName());
        });
    }

    private void registerTimeLimiterEvents() {
        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter(REDIS_LEASE_RESILIENCE_NAME);
        timeLimiter.getEventPublisher().onEvent(event -> {
            String eventType = event.getEventType().name();
            if ("TIMEOUT".equals(eventType) || "ERROR".equals(eventType)) {
                log.warn("redis timeLimiter event={} name={}", eventType, event.getTimeLimiterName());
                return;
            }
            log.debug("redis timeLimiter event={} name={}", eventType, event.getTimeLimiterName());
        });
    }

    private void registerRetryEvents() {
        Retry retry = retryRegistry.retry(REDIS_LEASE_RESILIENCE_NAME);
        retry.getEventPublisher().onEvent(event -> {
            String eventType = event.getEventType().name();
            if ("RETRY".equals(eventType)) {
                log.info("redis retry event={} name={} attempts={}", eventType, event.getName(), event.getNumberOfRetryAttempts());
                return;
            }
            if ("ERROR".equals(eventType)) {
                log.warn("redis retry event={} name={} attempts={}", eventType, event.getName(), event.getNumberOfRetryAttempts());
                return;
            }
            log.debug("redis retry event={} name={} attempts={}", eventType, event.getName(), event.getNumberOfRetryAttempts());
        });
    }

    private void registerCircuitBreakerEvents() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(REDIS_LEASE_RESILIENCE_NAME);
        circuitBreaker.getEventPublisher().onEvent(event -> {
            String eventType = event.getEventType().name();
            if ("STATE_TRANSITION".equals(eventType) || "CALL_NOT_PERMITTED".equals(eventType) || "ERROR".equals(eventType)) {
                log.warn("redis circuitBreaker event={} name={}", eventType, event.getCircuitBreakerName());
                return;
            }
            log.debug("redis circuitBreaker event={} name={}", eventType, event.getCircuitBreakerName());
        });
    }

    private void registerCircuitBreakerEventsPaymentGateway() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("paymentService");
        circuitBreaker.getEventPublisher().onEvent(event -> {
            String eventType = event.getEventType().name();
            if ("STATE_TRANSITION".equals(eventType) || "CALL_NOT_PERMITTED".equals(eventType) || "ERROR".equals(eventType)) {
                log.warn("paymentService circuitBreaker event={} name={}", eventType, event.getCircuitBreakerName());
                return;
            }
            log.debug("paymentService circuitBreaker event={} name={}", eventType, event.getCircuitBreakerName());
        });
    }

    private void registerCircuitBreakerEventsOrderGateway() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("orderService");
        circuitBreaker.getEventPublisher().onEvent(event -> {
            String eventType = event.getEventType().name();
            if ("STATE_TRANSITION".equals(eventType) || "CALL_NOT_PERMITTED".equals(eventType) || "ERROR".equals(eventType)) {
                log.warn("orderService circuitBreaker event={} name={}", eventType, event.getCircuitBreakerName());
                return;
            }
            log.debug("orderService circuitBreaker event={} name={}", eventType, event.getCircuitBreakerName());
        });
    }
}
