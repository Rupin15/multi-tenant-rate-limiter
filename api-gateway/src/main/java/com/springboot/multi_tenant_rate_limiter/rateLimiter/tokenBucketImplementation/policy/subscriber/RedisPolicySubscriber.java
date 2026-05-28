package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.subscriber;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.luaScripting.RedisHealthState;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.events.RateLimitPolicyEvent;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.services.RateLimitPolicyRegistry;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisPolicySubscriber {
    private static final String CHANNEL = "rate-limit-policy-updates";
    private final RedisMessageListenerContainer listenerContainer;
    private final RedisHealthState redisHealthState;
    private final RateLimitPolicyRegistry registry;
    private final ObjectMapper objectMapper;
    private final Map<String, Instant> processedEvents = new ConcurrentHashMap<>();

    @PostConstruct
    public void subscribe() {
        MessageListener listener = this::handleMessage;
        listenerContainer.addMessageListener(listener, new ChannelTopic(CHANNEL));
        log.info("Subscribed to redis channel={}", CHANNEL);
    }

    @PreDestroy
    public void stopListenerContainer() {
        if (listenerContainer.isRunning()) {
            listenerContainer.stop();
        }
    }

    private void handleMessage(Message message, byte[] pattern) {
        try {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            RateLimitPolicyEvent event = objectMapper.readValue(payload, RateLimitPolicyEvent.class);
            if (processedEvents.containsKey(event.getEventId())) {
                log.debug("Ignoring duplicate event={}", event.getEventId());
                return;
            }
            processedEvents.put(event.getEventId(), Instant.now());
            registry.refreshPolicy(event.getPolicyName(), event.getPolicy());
            log.info("Updated local cache route={} version={}", event.getPolicyName(), event.getVersion());
            redisHealthState.markHealthy();
        } catch (Exception ex) {
            redisHealthState.markUnhealthy();
            log.error("Failed processing redis policy event", ex
            );
        }
    }

    public Map<String, Instant> getProcessedEvents() {
        return processedEvents;
    }
}
