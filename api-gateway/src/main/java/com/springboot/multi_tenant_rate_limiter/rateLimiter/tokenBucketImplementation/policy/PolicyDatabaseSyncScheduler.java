package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy;

import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.repository.RateLimitPolicyEntity;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.repository.RateLimitPolicyRepository;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.services.RateLimitPolicyRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PolicyDatabaseSyncScheduler {

    private final RateLimitPolicyRepository repository;

    private final RateLimitPolicyRegistry registry;

    private volatile OffsetDateTime lastSyncTime = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(10);

    @Scheduled(fixedDelay = 10*60*1000)
    public void syncPoliciesFromDatabase() {

        OffsetDateTime currentSyncTime =
                OffsetDateTime.now(ZoneOffset.UTC);

        try {
            List<RateLimitPolicyEntity> updatedPolicies = repository.findAllByLastUpdatedGreaterThanEqual(lastSyncTime);
            for (RateLimitPolicyEntity entity : updatedPolicies) {
                registry.refreshPolicy(entity.getRouteId(), entity.toPolicy());
                log.info("Synced policy from DB for route={}", entity.getRouteId());
            }
            lastSyncTime = currentSyncTime.minusSeconds(1);
        } catch (Exception ex) {
            log.error("Failed syncing policies from DB", ex);
        }
    }
}